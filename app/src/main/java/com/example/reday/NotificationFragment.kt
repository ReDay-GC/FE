package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationFragment : Fragment() {

    private lateinit var adapter: NotificationAdapter
    private var currentFilter = FilterType.ALL
    private var onlyUnread = false

    private enum class FilterType { ALL, SYSTEM, MY }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvNotifications = view.findViewById<RecyclerView>(R.id.rv_notifications)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty)
        val layoutFilter = view.findViewById<LinearLayout>(R.id.layout_filter)
        val filterDivider = view.findViewById<View>(R.id.filter_divider)
        val chipAll = view.findViewById<TextView>(R.id.chip_all)
        val chipSystem = view.findViewById<TextView>(R.id.chip_system)
        val chipMy = view.findViewById<TextView>(R.id.chip_my)
        val btnOnlyUnread = view.findViewById<View>(R.id.btn_only_unread)
        val ivOnlyUnreadIcon = view.findViewById<ImageView>(R.id.iv_only_unread_icon)

        val allItems = listOf(
            NotificationUiModel(1, NotificationType.AI_GENERATE, "AI 생성을 잊으셨나요?",
                "어제 추가한 기억이 아직 AI 생성되지 않았어요. 지금 완성해보세요!", "1시간 전", false),
            NotificationUiModel(2, NotificationType.INQUIRY, "문의하신 내용에 답변이 도착...",
                "\"앱 사용 방법 문의\"에 대한 답변을 확인해보세요", "18시간 전", false),
            NotificationUiModel(3, NotificationType.NOTICE, "새 공지사항이 등록되었어요",
                "새로운 기능이 추가되었어요. 지금 바로 써보세요!", "어제", true),
            NotificationUiModel(4, NotificationType.REMINDER, "오늘의 기억을 남겨보세요",
                "오늘 하루도 소중한 순간들이 있었을 거예요. 기억을 기록해보세요 📝", "어제", true),
            NotificationUiModel(5, NotificationType.NOTICE, "Re:Day 정기 점검 안내",
                "3월 25일 02:00~04:00 정기 점검이 예정되어 있습니다", "2일 전", true),
            NotificationUiModel(6, NotificationType.AI_GENERATE, "AI 생성을 잊으셨나요?",
                "3월 15일에 추가한 기억이 아직 완성되지 않았어요", "3일 전", true)
        )

        adapter = NotificationAdapter(allItems)
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter = adapter

        fun filteredItems(): List<NotificationUiModel> {
            val base = when (currentFilter) {
                FilterType.ALL -> allItems
                FilterType.SYSTEM -> allItems.filter {
                    it.type == NotificationType.NOTICE || it.type == NotificationType.INQUIRY
                }
                FilterType.MY -> allItems.filter {
                    it.type == NotificationType.AI_GENERATE || it.type == NotificationType.REMINDER
                }
            }
            return if (onlyUnread) base.filter { !it.isRead } else base
        }

        fun applyFilter() {
            val items = filteredItems()
            if (items.isEmpty()) {
                rvNotifications.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                layoutFilter.visibility = View.GONE
                filterDivider.visibility = View.GONE
            } else {
                rvNotifications.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
                layoutFilter.visibility = View.VISIBLE
                filterDivider.visibility = View.VISIBLE
            }
            adapter.updateItems(items)
        }

        fun updateChipUI() {
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

        fun updateOnlyUnreadUI() {
            btnOnlyUnread.alpha = if (onlyUnread) 1.0f else 0.4f
            ivOnlyUnreadIcon.setImageResource(R.drawable.ic_check_stroke)
        }

        chipAll.setOnClickListener {
            currentFilter = FilterType.ALL
            updateChipUI()
            applyFilter()
        }

        chipSystem.setOnClickListener {
            currentFilter = FilterType.SYSTEM
            updateChipUI()
            applyFilter()
        }

        chipMy.setOnClickListener {
            currentFilter = FilterType.MY
            updateChipUI()
            applyFilter()
        }

        btnOnlyUnread.setOnClickListener {
            onlyUnread = !onlyUnread
            updateOnlyUnreadUI()
            applyFilter()
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        applyFilter()
        updateChipUI()
        updateOnlyUnreadUI()
    }
}
