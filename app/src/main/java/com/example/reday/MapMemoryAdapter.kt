package com.example.reday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapMemoryAdapter(
    private val memories: List<MemoryUiModel>,
    private val onItemClick: ((MemoryUiModel) -> Unit)? = null
) : RecyclerView.Adapter<MapMemoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_map_card_thumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tv_map_card_title)
        val tvPreview: TextView = view.findViewById(R.id.tv_map_card_preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_map_memory_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memory = memories[position]
        holder.tvTitle.text = memory.title

        if (!memory.thumbnailPath.isNullOrEmpty()) {
            val scope = (holder.itemView.context as? LifecycleOwner)?.lifecycleScope
            scope?.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapWithCorrectOrientation(memory.thumbnailPath!!)
                }
                if (bitmap != null) {
                    holder.ivThumbnail.setImageBitmap(bitmap)
                    holder.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        } else {
            holder.ivThumbnail.setImageResource(android.R.color.transparent)
        }

        if (!memory.previewText.isNullOrEmpty()) {
            holder.tvPreview.text = memory.previewText
            holder.tvPreview.isVisible = true
        } else {
            holder.tvPreview.isVisible = false
        }

        holder.itemView.setOnClickListener { onItemClick?.invoke(memory) }
    }

    override fun getItemCount() = memories.size
}
