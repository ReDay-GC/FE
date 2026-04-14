package com.example.reday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class InquiryAnswer(
    val authorName: String,
    val date: String,
    val content: String
)

data class InquiryUiModel(
    val id: Int,
    val userName: String,
    val date: String,
    val title: String,
    val content: String,
    val answer: InquiryAnswer? = null
)

class InquiryAdapter(
    private val items: List<InquiryUiModel>
) : RecyclerView.Adapter<InquiryAdapter.InquiryViewHolder>() {

    inner class InquiryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_inquiry_title)
        val tvContent: TextView = itemView.findViewById(R.id.tv_inquiry_content)
        val layoutPending: LinearLayout = itemView.findViewById(R.id.layout_answer_pending)
        val layoutReceived: LinearLayout = itemView.findViewById(R.id.layout_answer_received)
        val tvAnswerAuthor: TextView = itemView.findViewById(R.id.tv_answer_author)
        val tvAnswerDate: TextView = itemView.findViewById(R.id.tv_answer_date)
        val tvAnswerContent: TextView = itemView.findViewById(R.id.tv_answer_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InquiryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inquiry, parent, false)
        return InquiryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InquiryViewHolder, position: Int) {
        val item = items[position]
        holder.tvUserName.text = item.userName
        holder.tvDate.text = item.date
        holder.tvTitle.text = item.title
        holder.tvContent.text = item.content

        if (item.answer == null) {
            holder.layoutPending.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
        } else {
            holder.layoutPending.visibility = View.GONE
            holder.layoutReceived.visibility = View.VISIBLE
            holder.tvAnswerAuthor.text = item.answer.authorName
            holder.tvAnswerDate.text = item.answer.date
            holder.tvAnswerContent.text = item.answer.content
        }
    }

    override fun getItemCount() = items.size
}
