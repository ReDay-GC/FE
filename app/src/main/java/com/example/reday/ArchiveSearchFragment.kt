package com.example.reday

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.data.remote.ParseSearchRequest
import com.example.reday.data.remote.ParseSearchResponse
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.MemoryTextItem
import com.example.reday.data.remote.SearchSemanticRequest
import com.example.reday.data.repository.MemoryRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ArchiveSearchFragment : Fragment() {

    private lateinit var repository: MemoryRepository
    private lateinit var adapter: SearchResultAdapter

    // AI 검색용 전체 기억 목록
    private var allItems: List<MemoryUiModel> = emptyList()

    private var isFilterOpen = false
    private val selectedTags = mutableSetOf<String>()
    private var selectedEmotion: String? = null
    private var aiFilter: ParseSearchResponse? = null
    private var semanticRankedIds: List<Long> = emptyList()

    private val fallbackTags = listOf("여행", "카페", "산책", "쇼핑", "문화생활", "운동", "유흥", "자연", "식사", "휴식", "공부")

    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_archive_search, container, false)

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.app_header)?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        activity?.findViewById<View>(R.id.app_header)?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MemoryRepository()

        adapter = SearchResultAdapter(emptyList()) { memory ->
            val intent = android.content.Intent(requireContext(), MemoryDetailActivity::class.java)
            intent.putExtra(MemoryDetailActivity.EXTRA_DATE, memory.date)
            intent.putExtra(MemoryDetailActivity.EXTRA_SERVER_ID, memory.id)
            startActivity(intent)
        }
        view.findViewById<RecyclerView>(R.id.rv_search_result).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@ArchiveSearchFragment.adapter
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupFilterButton(view)

        val etSearch = view.findViewById<EditText>(R.id.et_search)

        // 텍스트 변경 → 서버 키워드 검색 (300ms 디바운스)
        etSearch.addTextChangedListener {
            aiFilter = null
            semanticRankedIds = emptyList()
            val query = it?.toString() ?: ""
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                performServerSearch(query, selectedTags)
            }
        }

        // Enter → AI 자연어 검색
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text?.toString()?.trim() ?: ""
                if (query.isNotBlank()) {
                    searchJob?.cancel()
                    triggerAiSearch(query)
                }
                true
            } else false
        }

        // 전체 기억 로드 (AI 시맨틱 검색용) + 태그/감정 칩 셋업
        viewLifecycleOwner.lifecycleScope.launch {
            val entities = repository.getAllMemories()
            allItems = MemoryMapper.fromMemoryEntityList(entities)

            setupTagChips(view, fallbackTags)
            setupEmotionChips(view)
        }

        showResults(emptyList(), isInitial = true)

        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        etSearch.postDelayed({ imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT) }, 100)
    }

    // ── 서버 검색 ──

    private suspend fun performServerSearch(keyword: String, tags: Set<String>) {
        val emotion = selectedEmotion
        if (keyword.isBlank() && tags.isEmpty() && emotion == null) {
            showResults(emptyList(), isInitial = true)
            return
        }

        val results: List<MemoryUiModel> = when {
            emotion != null -> {
                // 감정 API 호출 후 태그/키워드로 클라이언트 필터
                var models = MemoryMapper.fromMemoryEntityList(repository.getMemoriesByEmotion(emotionToEnum(emotion)))
                if (tags.isNotEmpty()) {
                    models = models.filter { item -> tags.any { tag -> item.tags.contains(tag) } }
                }
                if (keyword.isNotBlank()) {
                    models = models.filter { item ->
                        item.title.contains(keyword, ignoreCase = true) ||
                        item.previewText?.contains(keyword, ignoreCase = true) == true
                    }
                }
                models
            }
            keyword.isNotBlank() && tags.isNotEmpty() -> {
                val entities = repository.searchByKeyword(keyword)
                val uiModels = MemoryMapper.fromMemoryEntityList(entities)
                uiModels.filter { item -> tags.any { tag -> item.tags.contains(tag) } }
            }
            keyword.isNotBlank() -> {
                MemoryMapper.fromMemoryEntityList(repository.searchByKeyword(keyword))
            }
            tags.isNotEmpty() -> {
                val tagResults = tags.flatMap { tag ->
                    repository.getMemoriesByTag(tag)
                }.distinctBy { it.date }
                MemoryMapper.fromMemoryEntityList(tagResults)
            }
            else -> emptyList()
        }

        showResults(results.sortedByDescending { it.date })
    }

    // ── AI 자연어 검색 ──

    private fun triggerAiSearch(query: String) {
        val pbLoading = view?.findViewById<View>(R.id.layout_ai_loading)
        pbLoading?.isVisible = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val parseJob = launch {
                    try {
                        aiFilter = RetrofitClient.memoryApi.parseSearch(ParseSearchRequest(query))
                    } catch (e: Exception) { /* 폴백: aiFilter null 유지 */ }
                }

                val semanticJob = launch {
                    try {
                        val memoryItems = allItems
                            .filter { it.id > 0 }
                            .map { item ->
                                val text = "${item.title} ${item.previewText.orEmpty()}"
                                MemoryTextItem(item.id, text.trim())
                            }
                        if (memoryItems.isNotEmpty()) {
                            val result = RetrofitClient.memoryApi.searchSemantic(
                                SearchSemanticRequest(query, memoryItems)
                            )
                            semanticRankedIds = result.ranked_ids
                        }
                    } catch (e: Exception) { /* 폴백 */ }
                }

                parseJob.join()
                semanticJob.join()
                applyAiFilter(query)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "AI 검색에 실패했어요. 일반 검색으로 대신할게요.", Toast.LENGTH_SHORT).show()
                performServerSearch(query, selectedTags)
            } finally {
                pbLoading?.visibility = View.GONE
            }
        }
    }

    private fun applyAiFilter(query: String) {
        val positiveEmotions = setOf("😊 즐거운", "🥰 설레는", "😌 평온한", "🤩 신나는")
        val negativeEmotions = setOf("😤 지친", "😢 힘든")

        val filtered = allItems.filter { item ->
            val ai = aiFilter

            val matchesQuery = if (ai != null) {
                val matchesPeople = ai.people.isEmpty() ||
                    ai.people.any { p -> item.people.any { it.contains(p, ignoreCase = true) || p.contains(it, ignoreCase = true) } }
                val matchesTags = ai.tags.isEmpty() ||
                    ai.tags.any { t -> item.tags.contains(t) }
                val matchesLocations = ai.locations.isEmpty() ||
                    ai.locations.any { l -> item.locationName?.contains(l, ignoreCase = true) == true }
                val matchesYearMonth = ai.yearMonth == null ||
                    item.date.startsWith(ai.yearMonth)
                val matchesKeywords = ai.keywords.isEmpty() ||
                    ai.keywords.any { k ->
                        item.title.contains(k, ignoreCase = true) ||
                        item.previewText?.contains(k, ignoreCase = true) == true ||
                        item.locationName?.contains(k, ignoreCase = true) == true ||
                        item.people.any { p -> p.contains(k, ignoreCase = true) } ||
                        item.tags.any { t -> t.contains(k, ignoreCase = true) }
                    }
                matchesPeople && matchesTags && matchesLocations && matchesYearMonth && matchesKeywords
            } else {
                query.isBlank() || item.title.contains(query, ignoreCase = true)
            }

            val matchesSentiment = when (aiFilter?.sentiment) {
                "긍정" -> item.emotion in positiveEmotions
                "부정" -> item.emotion in negativeEmotions
                else -> true
            }

            val matchesTags = selectedTags.isEmpty() ||
                selectedTags.any { tag -> item.tags.contains(tag) }

            val passesContent = if (semanticRankedIds.isNotEmpty()) {
                item.id in semanticRankedIds
            } else {
                matchesQuery
            }

            matchesTags && passesContent && matchesSentiment
        }

        val sorted = if (semanticRankedIds.isNotEmpty()) {
            val rankMap = semanticRankedIds.mapIndexed { idx, id -> id to idx }.toMap()
            filtered.sortedBy { rankMap[it.id] ?: Int.MAX_VALUE }
        } else {
            filtered.sortedByDescending { it.date }
        }

        showResults(sorted)
    }

    // ── 공통 결과 표시 ──

    private fun showResults(items: List<MemoryUiModel>, isInitial: Boolean = false) {
        adapter.updateList(if (isInitial) emptyList() else items)
        val tvTitle = view?.findViewById<TextView>(R.id.tv_empty_title)
        val tvSubtitle = view?.findViewById<TextView>(R.id.tv_empty_subtitle)

        when {
            isInitial -> {
                tvTitle?.text = "기억을 검색해보세요"
                tvSubtitle?.text = "제목, 장소, 태그, 사람 등으로 검색할 수 있어요"
                view?.findViewById<View>(R.id.layout_empty)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.layout_result)?.visibility = View.GONE
            }
            items.isEmpty() -> {
                tvTitle?.text = "검색 결과가 없습니다"
                tvSubtitle?.text = "다른 검색이나 필터를 시도해보세요"
                view?.findViewById<View>(R.id.layout_empty)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.layout_result)?.visibility = View.GONE
            }
            else -> {
                view?.findViewById<View>(R.id.layout_empty)?.visibility = View.GONE
                view?.findViewById<View>(R.id.layout_result)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.tv_result_count)?.text = "총 ${items.size}개의 검색 결과"
            }
        }
    }

    // ── 필터 / 태그 칩 ──

    private fun setupFilterButton(view: View) {
        val btnFilter = view.findViewById<FrameLayout>(R.id.btn_filter)
        val ivFilterIcon = view.findViewById<ImageView>(R.id.iv_filter_icon)
        val layoutPanel = view.findViewById<View>(R.id.layout_filter_panel)
        val divider = view.findViewById<View>(R.id.divider_filter)

        btnFilter.setOnClickListener {
            isFilterOpen = !isFilterOpen
            layoutPanel.visibility = if (isFilterOpen) View.VISIBLE else View.GONE
            divider.visibility = if (isFilterOpen) View.VISIBLE else View.GONE

            if (isFilterOpen) {
                btnFilter.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_active)
                ivFilterIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.sub_200))
            } else {
                btnFilter.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_search_bar)
                ivFilterIcon.clearColorFilter()
            }
        }
    }

    private fun setupEmotionChips(view: View) {
        val emotions = listOf("😊 즐거운", "🥰 설레는", "😌 평온한", "🤩 신나는", "🤢 지친", "😰 힘든", "😡 화난", "😐 평범한")
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filter_tags)
        emotions.forEach { emotion ->
            chipGroup.addView(createEmotionChip(emotion, chipGroup))
        }
    }

    private fun createEmotionChip(emotion: String, chipGroup: ChipGroup): Chip {
        return Chip(requireContext()).apply {
            text = emotion
            tag = "emotion"
            isCheckable = true
            isChecked = false
            chipStrokeWidth = 2f
            textSize = 12f
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(999f).build()
            updateChipStyle(this, false)

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // 감정 칩만 단일 선택 — 태그 칩은 건드리지 않음
                    for (i in 0 until chipGroup.childCount) {
                        val other = chipGroup.getChildAt(i) as? Chip
                        if (other != null && other != this && other.tag == "emotion" && other.isChecked) {
                            other.isChecked = false
                        }
                    }
                    selectedEmotion = emotion
                } else {
                    if (selectedEmotion == emotion) selectedEmotion = null
                }
                updateChipStyle(this, isChecked)
                val etSearch = view?.findViewById<EditText>(R.id.et_search)
                val query = etSearch?.text?.toString() ?: ""
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    performServerSearch(query, selectedTags)
                }
            }
        }
    }

    private fun emotionToEnum(emotion: String): String = when {
        emotion.contains("즐거") -> "HAPPY"
        emotion.contains("설레") -> "EXCITED"
        emotion.contains("평온") -> "CONTENT"
        emotion.contains("신나") -> "EXCITED"
        emotion.contains("지친") -> "SAD"
        emotion.contains("힘든") -> "SAD"
        emotion.contains("화난") -> "ANGRY"
        emotion.contains("평범") -> "NEUTRAL"
        else -> emotion
    }

    private fun setupTagChips(view: View, tags: List<String>) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filter_tags)
        chipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = createFilterChip(tag)
            chipGroup.addView(chip)
        }
    }

    private fun createFilterChip(tag: String): Chip {
        return Chip(requireContext()).apply {
            text = "#$tag"
            isCheckable = true
            isChecked = false
            chipStrokeWidth = 2f
            textSize = 12f
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(999f).build()
            updateChipStyle(this, false)

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTags.add(tag) else selectedTags.remove(tag)
                updateChipStyle(this, isChecked)
                val etSearch = view?.findViewById<EditText>(R.id.et_search)
                val query = etSearch?.text?.toString() ?: ""
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    performServerSearch(query, selectedTags)
                }
            }
        }
    }

    private fun updateChipStyle(chip: Chip, selected: Boolean) {
        if (selected) {
            chip.setChipBackgroundColorResource(R.color.sub_105)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.sub_105))
        } else {
            chip.setChipBackgroundColorResource(R.color.brown_50)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.brown_300))
        }
    }
}
