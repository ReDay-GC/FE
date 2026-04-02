package com.example.reday

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.model.MemoryUiModel
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

        adapter = SearchResultAdapter(emptyList())
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
        etSearch.addTextChangedListener { applyFilter(etSearch.text?.toString() ?: "") }

        // 전체 기억 로드
        lifecycleScope.launch {
            repository.getAllMemories().collectLatest { entities ->
                allItems = MemoryMapper.fromMemoryEntityList(entities)
                applyFilter(etSearch.text?.toString() ?: "")
            }
        }

        // 키보드 자동 표시
        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        etSearch.postDelayed({ imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT) }, 100)
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
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.previewText?.contains(query, ignoreCase = true) == true ||
                item.tags.any { it.contains(query, ignoreCase = true) } ||
                item.people.any { it.contains(query, ignoreCase = true) } ||
                item.locationName?.contains(query, ignoreCase = true) == true

            val matchesTags = selectedTags.isEmpty() ||
                selectedTags.any { tag -> item.tags.contains(tag) }

            matchesQuery && matchesTags
        }

        adapter.updateList(filtered)

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
