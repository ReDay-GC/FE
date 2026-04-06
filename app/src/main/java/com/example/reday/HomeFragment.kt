package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.repository.RecordFragmentRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var repository: RecordFragmentRepository
    private lateinit var adapter: MemoryCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(requireContext())
        repository = RecordFragmentRepository(db.recordFragmentDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTodayDate(view)
        setupAddMemoryButton(view)
        setupRecyclerView(view)
        observeMemories(view)
    }

    private fun setupAddMemoryButton(view: View) {
        view.findViewById<View>(R.id.btn_add_memory).setOnClickListener {
            startActivity(Intent(requireContext(), AddMemoryActivity::class.java))
        }
    }

    private fun startAddMemoryForToday() {
        val cal = Calendar.getInstance()
        val intent = Intent(requireContext(), AddMemoryActivity::class.java).apply {
            putExtra(AddMemoryActivity.EXTRA_YEAR, cal.get(Calendar.YEAR))
            putExtra(AddMemoryActivity.EXTRA_MONTH, cal.get(Calendar.MONTH) + 1)
            putExtra(AddMemoryActivity.EXTRA_DAY, cal.get(Calendar.DAY_OF_MONTH))
        }
        startActivity(intent)
    }

    private fun setTodayDate(view: View) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = arrayOf("일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        view.findViewById<TextView>(R.id.tv_date).text = "${year}년 ${month}월 ${day}일 ${dayOfWeek}요일"
    }

    private fun setupRecyclerView(view: View) {
        adapter = MemoryCardAdapter()
        view.findViewById<RecyclerView>(R.id.rv_memories).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun observeMemories(view: View) {
        val today = run {
            val cal = Calendar.getInstance()
            "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllFragments().collectLatest { allFragments ->
                val memories = MemoryMapper.fromFragmentList(allFragments, limit = 3)

                // 오늘의 기억 기록 수
                val todayCount = allFragments.count { it.date == today }
                view.findViewById<TextView>(R.id.tv_record_count).text = "${todayCount}개 기록"

                // 오늘 기록 유무에 따라 힌트 텍스트 변경
                val tvHint = view.findViewById<TextView>(R.id.tv_add_record_hint)
                tvHint.text = if (todayCount > 0) "+ 이어서 기록을 추가해보세요" else "+ 첫 기록을 추가해보세요"
                tvHint.setOnClickListener { startAddMemoryForToday() }

                // 최근 기억 목록
                val emptyCard = view.findViewById<View>(R.id.card_empty_memories)
                val recyclerView = view.findViewById<RecyclerView>(R.id.rv_memories)
                if (memories.isEmpty()) {
                    emptyCard.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyCard.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(memories)
                }
            }
        }
    }
}
