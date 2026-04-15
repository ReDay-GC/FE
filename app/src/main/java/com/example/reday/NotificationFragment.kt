package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.reday.data.remote.NotificationDto
import com.example.reday.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationFragment : Fragment() {

    private lateinit var adapter: NotificationAdapter
    private lateinit var rvNotifications: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutFilter: LinearLayout
    private lateinit var filterDivider: View
    private lateinit var chipAll: TextView
    private lateinit var chipSystem: TextView
    private lateinit var chipMy: TextView
    private lateinit var btnOnlyUnread: View
    private lateinit var ivOnlyUnreadIcon: ImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var currentFilter = FilterType.ALL
    private var onlyUnread = false
    private var allItems: List<NotificationUiModel> = emptyList()

    private enum class FilterType(val apiCategory: String) {
        ALL("ALL"), SYSTEM("SYSTEM"), MY("MY")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotifications = view.findViewById(R.id.rv_notifications)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        layoutFilter = view.findViewById(R.id.layout_filter)
        filterDivider = view.findViewById(R.id.filter_divider)
        chipAll = view.findViewById(R.id.chip_all)
        chipSystem = view.findViewById(R.id.chip_system)
        chipMy = view.findViewById(R.id.chip_my)
        btnOnlyUnread = view.findViewById(R.id.btn_only_unread)
        ivOnlyUnreadIcon = view.findViewById(R.id.iv_only_unread_icon)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.main_200)
        swipeRefresh.setOnRefreshListener { fetchNotifications() }

        adapter = NotificationAdapter(emptyList()) { item ->
            if (!item.isRead) markAsRead(item)
            navigateByType(item)
        }
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter = adapter

        chipAll.setOnClickListener { changeFilter(FilterType.ALL) }
        chipSystem.setOnClickListener { changeFilter(FilterType.SYSTEM) }
        chipMy.setOnClickListener { changeFilter(FilterType.MY) }

        btnOnlyUnread.setOnClickListener {
            onlyUnread = !onlyUnread
            updateOnlyUnreadUI()
            applyFilter()
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        updateChipUI()
        updateOnlyUnreadUI()
    }

    override fun onResume() {
        super.onResume()
        fetchNotifications()
    }

    private fun changeFilter(filter: FilterType) {
        currentFilter = filter
        updateChipUI()
        fetchNotifications()
    }

    private fun fetchNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationApi.getNotifications(currentFilter.apiCategory)
                Log.d("NotificationFragment", "API success: ${response.data.size}개")
                allItems = response.data.map { it.toUiModel() }
                applyFilter()
            } catch (e: Exception) {
                Log.e("NotificationFragment", "API 실패: ${e.javaClass.simpleName} - ${e.message}")
                applyFilter()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun markAsRead(item: NotificationUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.notificationApi.markAsRead(item.id)
                allItems = allItems.map { if (it.id == item.id) it.copy(isRead = true) else it }
                applyFilter()
                (activity as? MainActivity)?.updateNotificationBadge()
            } catch (_: Exception) {
                // 실패 시 무시 (다음 새로고침 시 서버 상태 반영)
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (onlyUnread) allItems.filter { !it.isRead } else allItems
        layoutFilter.visibility = View.VISIBLE
        filterDivider.visibility = View.VISIBLE
        if (filtered.isEmpty()) {
            rvNotifications.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvNotifications.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
        adapter.updateItems(filtered)
    }

    private fun updateChipUI() {
        val ctx = requireContext()
        chipAll.background = ContextCompat.getDrawable(ctx,
            if (currentFilter == FilterType.ALL) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chipAll.setTextColor(ContextCompat.getColor(ctx, R.color.brown_600))

        chipSystem.background = ContextCompat.getDrawable(ctx,
            if (currentFilter == FilterType.SYSTEM) R.drawable.bg_chip_selected_system else R.drawable.bg_chip_unselected)
        chipSystem.setTextColor(ContextCompat.getColor(ctx,
            if (currentFilter == FilterType.SYSTEM) R.color.brown_50 else R.color.brown_600))

        chipMy.background = ContextCompat.getDrawable(ctx,
            if (currentFilter == FilterType.MY) R.drawable.bg_chip_selected_my else R.drawable.bg_chip_unselected)
        chipMy.setTextColor(ContextCompat.getColor(ctx,
            if (currentFilter == FilterType.MY) R.color.brown_50 else R.color.brown_600))
    }

    private fun updateOnlyUnreadUI() {
        btnOnlyUnread.alpha = if (onlyUnread) 1.0f else 0.4f
        ivOnlyUnreadIcon.setImageResource(R.drawable.ic_check_stroke)
    }

    private fun navigateByType(item: NotificationUiModel) {
        when (item.type) {
            NotificationType.INQUIRY -> {
                val fragment = item.relatedId?.let { id ->
                    InquiryDetailFragment().apply {
                        arguments = Bundle().apply {
                            putLong(InquiryDetailFragment.ARG_INQUIRY_ID, id)
                            putString(InquiryDetailFragment.ARG_TITLE, item.title)
                        }
                    }
                } ?: run {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "문의 목록에서 해당 문의를 확인해주세요",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    InquiryFragment()
                }
                navigateToFragment(fragment)
            }
            NotificationType.NOTICE -> {
                val fragment = item.relatedId?.let { id ->
                    NoticeDetailFragment().apply {
                        arguments = Bundle().apply {
                            putLong(NoticeDetailFragment.ARG_NOTICE_ID, id)
                            putString(NoticeDetailFragment.ARG_TITLE, item.title)
                        }
                    }
                } ?: run {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "공지 목록에서 해당 공지를 확인해주세요",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    NoticeFragment()
                }
                navigateToFragment(fragment)
            }
            NotificationType.AI_GENERATE -> {
                val memoryId = item.relatedId ?: return
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.springMemoryApi.getMemoryDetail(memoryId)
                        val date = response.data?.memoryDate ?: return@launch
                        startActivity(
                            Intent(requireContext(), MemoryFragmentActivity::class.java).apply {
                                putExtra(MemoryFragmentActivity.EXTRA_DATE, date)
                            }
                        )
                    } catch (_: Exception) {
                        navigateToFragment(ArchiveFragment())
                    }
                }
            }
            NotificationType.REMINDER -> {
                val parts = item.date.split("-")
                if (parts.size == 3) {
                    startActivity(
                        Intent(requireContext(), AddMemoryActivity::class.java).apply {
                            putExtra(AddMemoryActivity.EXTRA_YEAR, parts[0].toInt())
                            putExtra(AddMemoryActivity.EXTRA_MONTH, parts[1].toInt())
                            putExtra(AddMemoryActivity.EXTRA_DAY, parts[2].toInt())
                        }
                    )
                }
            }
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.content_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun NotificationDto.toUiModel(): NotificationUiModel {
        val type = when (this.type) {
            "AI_GENERATION" -> NotificationType.AI_GENERATE
            "DAILY_RECORD" -> NotificationType.REMINDER
            "INQUIRY_ANSWER" -> NotificationType.INQUIRY
            "NOTICE" -> NotificationType.NOTICE
            else -> NotificationType.NOTICE
        }
        return NotificationUiModel(
            id = notificationId,
            type = type,
            title = title,
            body = content,
            timeLabel = formatRelativeTime(createdAt),
            isRead = isRead,
            relatedId = relatedId,
            date = createdAt.take(10)
        )
    }

    private fun formatRelativeTime(dateTime: String): String {
        return try {
            val parsed = ZonedDateTime.parse(dateTime).toLocalDateTime()
            val now = LocalDateTime.now()
            val hours = ChronoUnit.HOURS.between(parsed, now)
            val days = ChronoUnit.DAYS.between(parsed, now)
            when {
                hours < 1 -> "방금 전"
                hours < 24 -> "${hours}시간 전"
                days == 1L -> "어제"
                days < 7 -> "${days}일 전"
                else -> "${parsed.monthValue}.${parsed.dayOfMonth.toString().padStart(2, '0')}"
            }
        } catch (_: Exception) {
            dateTime.take(10)
        }
    }
}
