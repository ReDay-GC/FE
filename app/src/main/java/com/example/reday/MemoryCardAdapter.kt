package com.example.reday

import com.bumptech.glide.Glide
import com.example.reday.utils.toEmotionEmoji
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.ImageViewCompat
import com.example.reday.data.model.MemoryUiModel

class MemoryCardAdapter(
    private var items: List<MemoryUiModel> = emptyList(),
    private val onItemClick: ((MemoryUiModel) -> Unit)? = null
) : RecyclerView.Adapter<MemoryCardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tv_memory_title)
        val tvEmotion: TextView = view.findViewById(R.id.tv_emotion)
        val tvCount: TextView = view.findViewById(R.id.tv_fragment_count)
        val tvLocationDate: TextView = view.findViewById(R.id.tv_location_date)
        val tvPreview: TextView = view.findViewById(R.id.tv_preview_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memory_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvCount.text = item.fragmentCount.toString()

        val emotionEmoji = item.emotion.toEmotionEmoji()
        if (emotionEmoji != null) {
            holder.tvEmotion.text = emotionEmoji
            holder.tvEmotion.visibility = View.VISIBLE
        } else {
            holder.tvEmotion.visibility = View.GONE
        }

        val locationDate = buildString {
            if (!item.locationName.isNullOrBlank()) {
                append(item.locationName)
                append(" · ")
            }
            append(item.date)
        }
        holder.tvLocationDate.text = locationDate

        if (!item.previewText.isNullOrBlank()) {
            holder.tvPreview.text = item.previewText
            holder.tvPreview.visibility = View.VISIBLE
        } else {
            holder.tvPreview.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }

        if (!item.thumbnailPath.isNullOrBlank()) {
            holder.ivThumbnail.visibility = View.VISIBLE
            val thumbnailPath = item.thumbnailPath!!
            val source: Any = if (thumbnailPath.startsWith("http")) thumbnailPath else java.io.File(thumbnailPath)
            ImageViewCompat.setImageTintList(holder.ivThumbnail, null)
            Glide.with(holder.itemView).load(source).centerCrop().placeholder(R.drawable.ic_nav_archive).into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<MemoryUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }

}
