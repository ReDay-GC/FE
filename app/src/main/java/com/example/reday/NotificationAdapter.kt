package com.example.reday

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

enum class NotificationType { AI_GENERATE, INQUIRY, NOTICE, REMINDER }

data class NotificationUiModel(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val timeLabel: String,
    val isRead: Boolean
)

class NotificationAdapter(
    private var items: List<NotificationUiModel>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card_notification)
        val iconContainer: FrameLayout = view.findViewById(R.id.icon_container)
        val ivIcon: ImageView = view.findViewById(R.id.iv_notification_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_notification_title)
        val tvBody: TextView = view.findViewById(R.id.tv_notification_body)
        val tvTime: TextView = view.findViewById(R.id.tv_notification_time)
        val dotUnread: View = view.findViewById(R.id.dot_unread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvBody.text = item.body
        holder.tvTime.text = item.timeLabel

        // 타입별 아이콘 배경 및 아이콘 설정
        val (iconBg, iconRes, strokeColor, dotBg) = when (item.type) {
            NotificationType.AI_GENERATE -> Quad(
                if (item.isRead) R.drawable.bg_circle_pink else R.drawable.bg_circle_main200,
                R.drawable.ic_star_ai,
                R.color.main_200,
                R.drawable.bg_dot_main200
            )
            NotificationType.INQUIRY -> Quad(
                R.drawable.bg_circle_sub200,
                R.drawable.ic_memo_fragment,
                R.color.sub_200,
                R.drawable.bg_circle_sub200
            )
            NotificationType.NOTICE -> Quad(
                R.drawable.bg_circle_sub105,
                R.drawable.ic_bell,
                R.color.inactive,
                null
            )
            NotificationType.REMINDER -> Quad(
                R.drawable.bg_circle_pink,
                R.drawable.ic_bell,
                R.color.inactive,
                null
            )
        }

        holder.iconContainer.background = ContextCompat.getDrawable(ctx, iconBg)
        holder.ivIcon.setImageResource(iconRes)

        // 읽음 상태에 따라 카드 테두리 및 그림자 설정
        if (item.isRead) {
            holder.card.strokeColor = ContextCompat.getColor(ctx, R.color.inactive)
            holder.card.cardElevation = 0f
            holder.dotUnread.visibility = View.GONE
        } else {
            holder.card.strokeColor = ContextCompat.getColor(ctx, strokeColor)
            holder.card.cardElevation = ctx.resources.displayMetrics.density * 2
            if (dotBg != null) {
                holder.dotUnread.background = ContextCompat.getDrawable(ctx, dotBg)
                holder.dotUnread.visibility = View.VISIBLE
            } else {
                holder.dotUnread.visibility = View.GONE
            }
        }

        // 읽음 상태에 따라 제목 색상 조정
        holder.tvTitle.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (item.isRead) R.color.brown_500 else R.color.brown_700
            )
        )
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<NotificationUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    private data class Quad(
        val iconBg: Int,
        val iconRes: Int,
        val strokeColor: Int,
        val dotBg: Int?
    )
}
