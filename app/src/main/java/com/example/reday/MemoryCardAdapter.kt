package com.example.reday

import android.graphics.BitmapFactory
import com.example.reday.utils.loadBitmapWithCorrectOrientation
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
    private var items: List<MemoryUiModel> = emptyList()
) : RecyclerView.Adapter<MemoryCardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tv_memory_title)
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

        if (!item.thumbnailPath.isNullOrBlank()) {
            val bitmap = loadBitmapWithCorrectOrientation(item.thumbnailPath!!)
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap)
                ImageViewCompat.setImageTintList(holder.ivThumbnail, null)
            } else {
                setDefaultThumbnail(holder.ivThumbnail)
            }
        } else {
            setDefaultThumbnail(holder.ivThumbnail)
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<MemoryUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun setDefaultThumbnail(iv: ImageView) {
        iv.setImageResource(R.drawable.ic_nav_archive)
        ImageViewCompat.setImageTintList(
            iv,
            android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(iv.context, R.color.brown_300)
            )
        )
    }
}
