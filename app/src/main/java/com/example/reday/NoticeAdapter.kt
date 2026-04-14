package com.example.reday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class NoticeUiModel(
    val id: Long,
    val title: String,
    val preview: String,
    val time: String,
    val isRead: Boolean,
    val date: String,
    val content: String
)

class NoticeAdapter(
    private val items: List<NoticeUiModel>,
    private val onClick: (NoticeUiModel) -> Unit
) : RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder>() {

    inner class NoticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_notice_title)
        val tvPreview: TextView = itemView.findViewById(R.id.tv_notice_preview)
        val tvTime: TextView = itemView.findViewById(R.id.tv_notice_time)
        val dotUnread: View = itemView.findViewById(R.id.dot_unread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notice, parent, false)
        return NoticeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvPreview.text = item.preview
        holder.tvTime.text = item.time
        holder.dotUnread.visibility = if (!item.isRead) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
