package com.example.reday

import android.content.Context
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
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.remote.DailyCommentRequest
import com.example.reday.data.remote.MemoryForComment
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.data.repository.RecordFragmentRepository
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.reday.utils.launchWithLoading
import com.example.reday.utils.launchWithRefresh
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var fragmentRepository: RecordFragmentRepository
    private lateinit var memoryRepository: MemoryRepository
    private lateinit var adapter: MemoryCardAdapter

    companion object {
        private var commentShownThisSession = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentRepository = RecordFragmentRepository()
        memoryRepository = MemoryRepository()
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
        showDailyCommentIfNeeded()
    }

    private fun showDailyCommentIfNeeded() {
        if (commentShownThisSession) return
        val prefs = requireContext().getSharedPreferences("daily_comment", Context.MODE_PRIVATE)
        val today = run {
            val cal = Calendar.getInstance()
            "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
        val savedDate = prefs.getString("date", "")
        val savedComment = prefs.getString("comment", "")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val memories = memoryRepository.getAllMemories().take(1)
                if (memories.isEmpty()) {
                    prefs.edit().remove("date").remove("comment").apply()
                    return@launch
                }

                if (savedDate == today && !savedComment.isNullOrBlank()) {
                    showCommentBottomSheet(savedComment)
                    return@launch
                }

                val gson = Gson()
                val listType = object : TypeToken<List<String>>() {}.type
                val memoriesForComment = memories.map { entity ->
                    val tags = try {
                        gson.fromJson<List<String>>(entity.tags, listType) ?: emptyList()
                    } catch (e: Exception) { emptyList() }
                    MemoryForComment(
                        title = entity.title,
                        summary = entity.summary,
                        tags = tags
                    )
                }

                val response = RetrofitClient.memoryApi.dailyComment(
                    DailyCommentRequest(memoriesForComment)
                )

                prefs.edit()
                    .putString("date", today)
                    .putString("comment", response.comment)
                    .apply()

                showCommentBottomSheet(response.comment)
            } catch (e: Exception) {
                // 서버 실패 시 조용히 무시
            }
        }
    }

    private fun showCommentBottomSheet(comment: String) {
        commentShownThisSession = true
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_daily_comment, null)
        sheetView.findViewById<TextView>(R.id.tv_daily_comment).text = comment
        sheetView.findViewById<View>(R.id.btn_close_comment).setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(sheetView)
        dialog.show()
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
        adapter = MemoryCardAdapter { memory ->
            val intent = android.content.Intent(requireContext(), MemoryDetailActivity::class.java)
            intent.putExtra(MemoryDetailActivity.EXTRA_DATE, memory.date)
            startActivity(intent)
        }
        view.findViewById<RecyclerView>(R.id.rv_memories).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun observeMemories(view: View) {
        val pb = view.findViewById<View>(R.id.pb_loading)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.main_200)
        swipeRefresh.setOnRefreshListener { launchWithRefresh(swipeRefresh) { loadData(view) } }

        launchWithLoading(pb) { loadData(view) }
    }

    private suspend fun loadData(view: View) {
        val today = run {
            val cal = Calendar.getInstance()
            "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        val todayFragments = fragmentRepository.getFragmentsByDate(today)
        val todayCount = todayFragments.size
        view.findViewById<TextView>(R.id.tv_record_count).text = "${todayCount}개 기록"
        val tvHint = view.findViewById<TextView>(R.id.tv_add_record_hint)
        tvHint.text = if (todayCount > 0) "+ 이어서 기록을 추가해보세요" else "+ 첫 기록을 추가해보세요"
        tvHint.setOnClickListener {
            if (todayCount > 0) {
                val intent = android.content.Intent(requireContext(), MemoryFragmentActivity::class.java).apply {
                    putExtra(MemoryFragmentActivity.EXTRA_DATE, today)
                }
                startActivity(intent)
            } else {
                startAddMemoryForToday()
            }
        }

        val entities = memoryRepository.getAllMemories()
        val memories = MemoryMapper.fromMemoryEntityList(entities, limit = 3)
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
