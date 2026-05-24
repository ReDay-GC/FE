package com.example.reday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.model.MemoryUiModel
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.reday.utils.toEmotionEmoji
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ArchiveMemoryAdapter(
    private var items: List<MemoryUiModel>,
    private val onItemClick: ((MemoryUiModel) -> Unit)? = null
) : RecyclerView.Adapter<ArchiveMemoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flThumbnail: FrameLayout = itemView.findViewById(R.id.fl_thumbnail)
        val llCardBody: LinearLayout = itemView.findViewById(R.id.ll_card_body)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvTitleBody: TextView = itemView.findViewById(R.id.tv_title_body)
        val tvLocationOverlay: TextView = itemView.findViewById(R.id.tv_location_overlay)
        val tvEmotion: TextView = itemView.findViewById(R.id.tv_emotion)
        val tvSummary: TextView = itemView.findViewById(R.id.tv_summary)
        val ivLocationIcon: ImageView = itemView.findViewById(R.id.iv_location_icon_meta)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvMetaDot: TextView = itemView.findViewById(R.id.tv_meta_dot)
        val ivPeopleIcon: ImageView = itemView.findViewById(R.id.iv_people_icon)
        val tvPeople: TextView = itemView.findViewById(R.id.tv_people)
        val chipGroupTags: ChipGroup = itemView.findViewById(R.id.chip_group_tags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archive_memory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 썸네일
        val density = holder.itemView.resources.displayMetrics.density
        if (!item.thumbnailPath.isNullOrBlank()) {
            holder.flThumbnail.visibility = View.VISIBLE
            holder.llCardBody.minimumHeight = 0
            holder.tvTitleBody.visibility = View.GONE
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
            holder.flThumbnail.visibility = View.GONE
            holder.llCardBody.minimumHeight = (200 * density + 0.5f).toInt()
            holder.tvTitleBody.text = item.title
            holder.tvTitleBody.visibility = View.VISIBLE
        }

        // 제목 (썸네일 오버레이)
        holder.tvTitle.text = item.title

        // 썸네일 위 위치
        if (item.locationName != null) {
            holder.tvLocationOverlay.text = item.locationName
            holder.tvLocationOverlay.visibility = View.VISIBLE
        } else {
            holder.tvLocationOverlay.visibility = View.GONE
        }

        // 감정 이모지
        val emotionEmoji = item.emotion.toEmotionEmoji()
        if (emotionEmoji != null) {
            holder.tvEmotion.text = emotionEmoji
            holder.tvEmotion.visibility = View.VISIBLE
        } else {
            holder.tvEmotion.visibility = View.GONE
        }

        // 요약
        holder.tvSummary.text = item.previewText ?: ""

        // 위치
        if (!item.locationName.isNullOrBlank()) {
            holder.ivLocationIcon.visibility = View.VISIBLE
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = item.locationName
        } else {
            holder.ivLocationIcon.visibility = View.GONE
            holder.tvLocation.visibility = View.GONE
        }

        // 인물
        if (item.people.isNotEmpty()) {
            holder.tvMetaDot.visibility = View.VISIBLE
            holder.ivPeopleIcon.visibility = View.VISIBLE
            holder.tvPeople.visibility = View.VISIBLE
            holder.tvPeople.text = item.people.joinToString(", ")
        } else {
            holder.tvMetaDot.visibility = View.GONE
            holder.ivPeopleIcon.visibility = View.GONE
            holder.tvPeople.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }

        // 태그
        holder.chipGroupTags.removeAllViews()
        item.tags.forEach { tag ->
            val chip = Chip(holder.itemView.context).apply {
                text = "#$tag"
                isClickable = false
                isCheckable = false
                setTextColor(holder.itemView.context.getColor(R.color.sub_200))
                setChipBackgroundColorResource(R.color.sub_100)
                chipStrokeWidth = 0f
                textSize = 11f
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(999f)
                    .build()
            }
            holder.chipGroupTags.addView(chip)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<MemoryUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
