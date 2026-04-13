package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.data.repository.MemoryRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class ArchiveFragment : Fragment() {

    private lateinit var repository: MemoryRepository
    private lateinit var adapter: ArchiveMemoryAdapter

    private var currentYear = 0
    private var currentMonth = 0
    private var allItems: List<MemoryUiModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_archive, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MemoryRepository()

        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH) + 1

        adapter = ArchiveMemoryAdapter(emptyList()) { memory ->
            val intent = android.content.Intent(requireContext(), MemoryDetailActivity::class.java)
            intent.putExtra(MemoryDetailActivity.EXTRA_DATE, memory.date)
            startActivity(intent)
        }
        view.findViewById<RecyclerView>(R.id.rv_archive).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ArchiveFragment.adapter
        }

        view.findViewById<TextView>(R.id.btn_prev_month).setOnClickListener {
            if (currentMonth == 1) { currentMonth = 12; currentYear-- }
            else currentMonth--
            updateMonthTitle()
            loadMemories()
        }

        view.findViewById<TextView>(R.id.btn_next_month).setOnClickListener {
            if (currentMonth == 12) { currentMonth = 1; currentYear++ }
            else currentMonth++
            updateMonthTitle()
            loadMemories()
        }

        // 검색란 클릭 → 검색 화면으로 이동
        view.findViewById<View>(R.id.et_search).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, ArchiveSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        updateMonthTitle()
        loadMemories()
    }

    private fun updateMonthTitle() {
        view?.findViewById<TextView>(R.id.tv_month_title)?.text =
            "${currentYear}년 ${currentMonth}월"
    }

    private fun loadMemories() {
        lifecycleScope.launch {
            val entities = repository.getMemoriesByMonth(currentYear, currentMonth)
            allItems = MemoryMapper.fromMemoryEntityList(entities)
            showList(allItems)
        }
    }

    private fun showList(items: List<MemoryUiModel>) {
        adapter.updateList(items)
        val isEmpty = items.isEmpty()
        view?.findViewById<View>(R.id.layout_empty)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.rv_archive)?.visibility =
            if (isEmpty) View.GONE else View.VISIBLE
    }
}
