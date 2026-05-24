package com.example.reday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.chip.Chip

class SearchResultAdapter(
    private var items: List<MemoryUiModel>,
    private val onItemClick: ((MemoryUiModel) -> Unit)? = null
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val flNoImageHeader: FrameLayout = itemView.findViewById(R.id.fl_no_image_header)
        val tvNoImageTitle: TextView = itemView.findViewById(R.id.tv_no_image_title)
        val llCardContent: LinearLayout = itemView.findViewById(R.id.ll_card_content)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val layoutLocation: LinearLayout = itemView.findViewById(R.id.layout_location)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val layoutTags: LinearLayout = itemView.findViewById(R.id.layout_tags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 썸네일
        if (!item.thumbnailPath.isNullOrBlank()) {
            holder.ivThumbnail.visibility = View.VISIBLE
            holder.flNoImageHeader.visibility = View.GONE
            holder.tvTitle.visibility = View.VISIBLE
            val scope = (holder.itemView.context as? LifecycleOwner)?.lifecycleScope
            scope?.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapWithCorrectOrientation(item.thumbnailPath!!)
                }
                if (bitmap != null) {
                    holder.ivThumbnail.setImageBitmap(bitmap)
                    holder.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        } else {
            holder.ivThumbnail.visibility = View.GONE
            holder.flNoImageHeader.visibility = View.VISIBLE
            holder.tvNoImageTitle.text = item.title
            holder.tvTitle.visibility = View.GONE
        }

        // 날짜 포맷: "2026-03-08" → "2026.03.08"
        holder.tvDate.text = item.date.replace("-", ".")

        // 제목
        holder.tvTitle.text = item.title

        // 위치
        if (item.locationName != null) {
            holder.layoutLocation.visibility = View.VISIBLE
            holder.tvLocation.text = item.locationName
        } else {
            holder.layoutLocation.visibility = View.GONE
        }

        // 태그 (최대 2개) + 기억조각수
        holder.layoutTags.removeAllViews()
        item.tags.take(2).forEach { tag ->
            val chip = Chip(holder.itemView.context).apply {
                text = "#$tag"
                isClickable = false
                isCheckable = false
                setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.sub_200))
                setChipBackgroundColorResource(R.color.sub_100)
                chipStrokeWidth = 0f
                textSize = 9f
                shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(999f).build()
                setPadding(0, 0, 0, 0)
            }
            holder.layoutTags.addView(chip)
        }

        if (item.fragmentCount > 0) {
            val tvCount = TextView(holder.itemView.context).apply {
                text = "+${item.fragmentCount}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.brown_300))
                setPadding(10, 0, 0, 0)
            }
            holder.layoutTags.addView(tvCount)
        }

        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<MemoryUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
