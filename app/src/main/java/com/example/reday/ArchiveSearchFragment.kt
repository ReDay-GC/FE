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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.data.remote.MemoryEmbeddingItem
import com.example.reday.data.remote.ParseSearchRequest
import com.example.reday.data.remote.ParseSearchResponse
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.SearchSemanticRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.reday.data.repository.MemoryRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ArchiveSearchFragment : Fragment() {

    private lateinit var repository: MemoryRepository
    private lateinit var adapter: SearchResultAdapter
    private var allItems: List<MemoryUiModel> = emptyList()

    private var isFilterOpen = false
    private val selectedTags = mutableSetOf<String>()
    private var aiFilter: ParseSearchResponse? = null
    private var semanticRankedIds: List<Long> = emptyList()

    private val allTags = listOf("여행", "카페", "산책", "쇼핑", "문화생활", "운동", "유흥", "자연", "식사", "휴식", "공부")

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

        val db = AppDatabase.getInstance(requireContext())
        repository = MemoryRepository(db.memoryDao())

        adapter = SearchResultAdapter(emptyList()) { memory ->
            val intent = android.content.Intent(requireContext(), MemoryDetailActivity::class.java)
            intent.putExtra(MemoryDetailActivity.EXTRA_DATE, memory.date)
            startActivity(intent)
        }
        view.findViewById<RecyclerView>(R.id.rv_search_result).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@ArchiveSearchFragment.adapter
        }

        // 뒤로가기
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 필터 버튼
        setupFilterButton(view)

        // 태그 칩 생성
        setupTagChips(view)

        // 검색 입력
        val etSearch = view.findViewById<EditText>(R.id.et_search)

        // 텍스트 변경 시 → 기존 단순 검색
        etSearch.addTextChangedListener {
            aiFilter = null
            semanticRankedIds = emptyList()
            applyFilter(etSearch.text?.toString() ?: "")
        }

        // 키보드 검색 버튼 → AI 자연어 검색
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text?.toString()?.trim() ?: ""
                if (query.isNotBlank()) triggerAiSearch(query)
                true
            } else false
        }

        // 전체 기억 로드
        lifecycleScope.launch {
            val entities = repository.getAllMemories()
            allItems = MemoryMapper.fromMemoryEntityList(entities)
            applyFilter(etSearch.text?.toString() ?: "")
        }

        // 키보드 자동 표시
        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        etSearch.postDelayed({ imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT) }, 100)
    }

    private fun triggerAiSearch(query: String) {
        val pbLoading = view?.findViewById<ProgressBar>(R.id.pb_ai_search)
        pbLoading?.isVisible = true

        lifecycleScope.launch {
            try {
                // 자연어 파싱 + 의미 검색 병렬 실행
                val parseJob = launch {
                    try {
                        aiFilter = RetrofitClient.memoryApi.parseSearch(ParseSearchRequest(query))
                    } catch (e: Exception) { /* 폴백: aiFilter null 유지 */ }
                }

                val semanticJob = launch {
                    try {
                        val gson = Gson()
                        val floatListType = object : TypeToken<List<Float>>() {}.type
                        val embeddingItems = allItems.mapNotNull { memory ->
                            val embeddingJson = repository.getEmbeddingById(memory.id) ?: return@mapNotNull null
                            val vector = try {
                                gson.fromJson<List<Float>>(embeddingJson, floatListType)
                            } catch (e: Exception) { return@mapNotNull null }
                            if (vector.isNotEmpty()) MemoryEmbeddingItem(memory.id, vector) else null
                        }
                        if (embeddingItems.isNotEmpty()) {
                            val result = RetrofitClient.memoryApi.searchSemantic(
                                SearchSemanticRequest(query, embeddingItems)
                            )
                            semanticRankedIds = result.ranked_ids
                        }
                    } catch (e: Exception) { /* 폴백: semanticRankedIds 빈 리스트 유지 */ }
                }

                parseJob.join()
                semanticJob.join()
                applyFilter(query)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "AI 검색에 실패했어요. 일반 검색으로 대신할게요.", Toast.LENGTH_SHORT).show()
                applyFilter(query)
            } finally {
                pbLoading?.isVisible = false
            }
        }
    }

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

    private fun setupTagChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filter_tags)
        allTags.forEach { tag ->
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
                applyFilter(etSearch?.text?.toString() ?: "")
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

    private fun applyFilter(query: String) {
        val isInitial = query.isBlank() && selectedTags.isEmpty()

        val filtered = if (isInitial) emptyList()
        else allItems.filter { item ->
            val ai = aiFilter

            val positiveEmotions = setOf("😊 즐거운", "🥰 설레는", "😌 평온한", "🤩 신나는")
            val negativeEmotions = setOf("😤 지친", "😢 힘든")

            val matchesQuery = if (ai != null) {
                // AI 파싱 결과로 필터링
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
                // 기존 단순 검색
                query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.previewText?.contains(query, ignoreCase = true) == true ||
                    item.tags.any { it.contains(query, ignoreCase = true) } ||
                    item.people.any { it.contains(query, ignoreCase = true) } ||
                    item.locationName?.contains(query, ignoreCase = true) == true
            }

            // sentiment 필터: semantic/keyword 경로 모두 적용
            val matchesSentiment = when (ai?.sentiment) {
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

        // 의미 검색 결과가 있으면 유사도 순으로 정렬
        val sorted = if (semanticRankedIds.isNotEmpty()) {
            val rankMap = semanticRankedIds.mapIndexed { idx, id -> id to idx }.toMap()
            filtered.sortedBy { rankMap[it.id] ?: Int.MAX_VALUE }
        } else filtered

        adapter.updateList(sorted)

        val tvTitle = view?.findViewById<TextView>(R.id.tv_empty_title)
        val tvSubtitle = view?.findViewById<TextView>(R.id.tv_empty_subtitle)

        when {
            isInitial -> {
                tvTitle?.text = "기억을 검색해보세요"
                tvSubtitle?.text = "제목, 장소, 태그, 사람 등으로 검색할 수 있어요"
                view?.findViewById<View>(R.id.layout_empty)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.layout_result)?.visibility = View.GONE
            }
            filtered.isEmpty() -> {
                tvTitle?.text = "검색 결과가 없습니다"
                tvSubtitle?.text = "다른 검색이나 필터를 시도해보세요"
                view?.findViewById<View>(R.id.layout_empty)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.layout_result)?.visibility = View.GONE
            }
            else -> {
                view?.findViewById<View>(R.id.layout_empty)?.visibility = View.GONE
                view?.findViewById<View>(R.id.layout_result)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.tv_result_count)?.text = "총 ${filtered.size}개의 검색 결과"
            }
        }
    }
}
