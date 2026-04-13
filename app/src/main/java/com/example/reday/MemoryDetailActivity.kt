package com.example.reday

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.data.repository.RecordFragmentRepository
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class MemoryDetailActivity : AppCompatActivity() {

    private lateinit var memoryRepository: MemoryRepository
    private lateinit var fragmentRepository: RecordFragmentRepository

    private lateinit var ivHero: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvLocationDot: TextView
    private lateinit var ivLocationIcon: ImageView
    private lateinit var tvSummary: TextView
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var tvFragmentCount: TextView
    private lateinit var llTimeline: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_detail)

        val date = intent.getStringExtra(EXTRA_DATE) ?: run { finish(); return }

        ivHero = findViewById(R.id.iv_hero)
        tvTitle = findViewById(R.id.tv_title)
        tvDate = findViewById(R.id.tv_date)
        tvLocation = findViewById(R.id.tv_location)
        tvLocationDot = findViewById(R.id.tv_location_dot)
        ivLocationIcon = findViewById(R.id.iv_location_icon)
        tvSummary = findViewById(R.id.tv_summary)
        chipGroupTags = findViewById(R.id.chip_group_tags)
        tvFragmentCount = findViewById(R.id.tv_fragment_count)
        llTimeline = findViewById(R.id.ll_timeline)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_more).setOnClickListener {
            showMoreMenu(date)
        }

        memoryRepository = MemoryRepository()
        fragmentRepository = RecordFragmentRepository()

        loadData(date)
    }

    override fun onResume() {
        super.onResume()
        val date = intent.getStringExtra(EXTRA_DATE) ?: return
        loadData(date)
    }

    private fun showMoreMenu(date: String) {
        val parts = date.split("-")
        if (parts.size < 3) return
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_memory_menu)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<android.view.View>(R.id.btn_menu_add_fragment).setOnClickListener {
            dialog.dismiss()
            val intent = android.content.Intent(this, AddMemoryActivity::class.java).apply {
                putExtra(AddMemoryActivity.EXTRA_YEAR, parts[0].toIntOrNull() ?: return@setOnClickListener)
                putExtra(AddMemoryActivity.EXTRA_MONTH, parts[1].toIntOrNull() ?: return@setOnClickListener)
                putExtra(AddMemoryActivity.EXTRA_DAY, parts[2].toIntOrNull() ?: return@setOnClickListener)
                putExtra(AddMemoryActivity.EXTRA_GO_TO_TIMELINE, true)
            }
            startActivity(intent)
        }

        dialog.findViewById<android.view.View>(R.id.btn_menu_delete_memory).setOnClickListener {
            dialog.dismiss()
            showDeleteMemoryDialog(date)
        }

        dialog.show()
    }

    private fun showDeleteMemoryDialog(date: String) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_confirm)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_title).text = "기억카드 삭제"
        dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_message).text = "이 기억을 삭제하면 복구할 수 없어요.\n정말 삭제할까요?"
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_confirm).setOnClickListener {
            dialog.dismiss()
            lifecycleScope.launch {
                // 기록 조각 먼저 삭제 (서버 + 로컬)
                val fragments = fragmentRepository.getFragmentsByDate(date)
                fragments.forEach { fragmentRepository.deleteFragment(it) }
                // 기억 삭제 (서버 + 로컬)
                memoryRepository.deleteMemoryByDate(date)
                finish()
            }
        }
        dialog.show()
    }

    private fun loadData(date: String) {
        lifecycleScope.launch {
            val entity = memoryRepository.getMemoryByDate(date) ?: run { finish(); return@launch }

            // 히어로 사진
            if (!entity.representativePhotoUrl.isNullOrBlank()) {
                val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    loadBitmapWithCorrectOrientation(entity.representativePhotoUrl!!)
                }
                if (bitmap != null) ivHero.setImageBitmap(bitmap)
            }

            // 제목
            tvTitle.text = entity.title

            // 날짜
            tvDate.text = formatDateLabel(date)

            // 위치
            val location = entity.representativeLocationName
                ?: parseLocations(entity.locations).firstOrNull()
            if (!location.isNullOrBlank()) {
                tvLocation.text = location
                tvLocation.visibility = View.VISIBLE
                tvLocationDot.visibility = View.VISIBLE
                ivLocationIcon.visibility = View.VISIBLE
            }

            // 요약
            tvSummary.text = entity.summary

            // 태그 — 상세 API에서 조회
            val tags = entity.serverId?.let { serverId ->
                memoryRepository.getMemoryDetail(serverId)
                    ?.tags?.map { it.tagName }
            } ?: parseTags(entity.tags)
            chipGroupTags.removeAllViews()
            tags?.forEach { tag ->
                chipGroupTags.addView(createTagChip(tag))
            }

            // 기록 조각 타임라인
            val fragments = fragmentRepository.getFragmentsByDate(date)
            tvFragmentCount.text = "${fragments.size}개"
            buildTimeline(fragments)
        }
    }

    private fun buildTimeline(fragments: List<RecordFragmentUiModel>) {
        llTimeline.removeAllViews()
        val sorted = fragments.sortedBy { it.createdAt }
        sorted.forEach { fragment ->
            llTimeline.addView(createFragmentItem(fragment))
        }
    }

    private fun createFragmentItem(fragment: RecordFragmentUiModel): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 12.dp }
        }

        // 타입 아이콘
        val icon = ImageView(this).apply {
            val size = 28.dp
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = 10.dp
                it.topMargin = 2.dp
            }
            when (fragment.fragmentType) {
                FragmentType.PHOTO -> {
                    setImageResource(R.drawable.ic_photo_fragment)
                    setBackgroundResource(R.drawable.bg_record_icon_photo)
                }
                FragmentType.TEXT -> {
                    setImageResource(R.drawable.ic_memo_fragment)
                    setBackgroundResource(R.drawable.bg_record_icon_square)
                }
                FragmentType.VOICE -> {
                    setImageResource(R.drawable.ic_mic)
                    setBackgroundResource(R.drawable.bg_record_icon_circle)
                    ImageViewCompat.setImageTintList(this,
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this@MemoryDetailActivity, R.color.main_200)))
                }
            }
            setPadding(4.dp, 4.dp, 4.dp, 4.dp)
        }
        row.addView(icon)

        // 콘텐츠
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 시간
        val timeText = TextView(this).apply {
            text = formatTimeShort(fragment.createdAt)
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@MemoryDetailActivity, R.color.brown_400))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4.dp }
        }
        content.addView(timeText)

        when (fragment.fragmentType) {
            FragmentType.PHOTO -> {
                if (!fragment.photoUrl.isNullOrBlank()) {
                    val imageView = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            200.dp
                        ).also { it.bottomMargin = 4.dp }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setBackgroundResource(R.drawable.bg_photo_preview_rounded)
                        clipToOutline = true
                    }
                    lifecycleScope.launch {
                        val bm = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            loadBitmapWithCorrectOrientation(fragment.photoUrl!!)
                        }
                        if (bm != null) imageView.setImageBitmap(bm)
                    }
                    content.addView(imageView)
                }
                if (!fragment.locationName.isNullOrBlank()) {
                    content.addView(createMetaText("@ ${fragment.locationName}"))
                }
                if (!fragment.contentText.isNullOrBlank()) {
                    content.addView(createBubbleText(fragment.contentText!!))
                }
            }
            FragmentType.TEXT -> {
                if (!fragment.locationName.isNullOrBlank()) {
                    content.addView(createMetaText("@ ${fragment.locationName}"))
                }
                if (!fragment.contentText.isNullOrBlank()) {
                    content.addView(createBubbleText(fragment.contentText!!))
                }
            }
            FragmentType.VOICE -> {
                if (!fragment.locationName.isNullOrBlank()) {
                    content.addView(createMetaText("@ ${fragment.locationName}"))
                }
                if (!fragment.contentText.isNullOrBlank()) {
                    content.addView(createBubbleText(fragment.contentText!!))
                }
            }
        }

        row.addView(content)
        return row
    }

    private fun createBubbleText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MemoryDetailActivity, R.color.brown_700))
            setBackgroundResource(R.drawable.bg_text_bubble)
            setPadding(12.dp, 8.dp, 12.dp, 8.dp)
            setLineSpacing(0f, 1.4f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4.dp }
        }
    }

    private fun createMetaText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@MemoryDetailActivity, R.color.brown_400))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4.dp }
        }
    }

    private fun createTagChip(tag: String): Chip {
        return Chip(this).apply {
            text = "#$tag"
            isClickable = false
            isCheckable = false
            setTextColor(ContextCompat.getColor(this@MemoryDetailActivity, R.color.sub_200))
            setChipBackgroundColorResource(R.color.sub_100)
            chipStrokeWidth = 0f
            textSize = 12f
        }
    }

    private fun parseTags(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun parseLocations(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun formatDateLabel(date: String): String = try {
        val ld = LocalDate.parse(date)
        val dow = when (ld.dayOfWeek) {
            DayOfWeek.MONDAY -> "월"
            DayOfWeek.TUESDAY -> "화"
            DayOfWeek.WEDNESDAY -> "수"
            DayOfWeek.THURSDAY -> "목"
            DayOfWeek.FRIDAY -> "금"
            DayOfWeek.SATURDAY -> "토"
            DayOfWeek.SUNDAY -> "일"
            else -> ""
        }
        "${ld.monthValue}월 ${ld.dayOfMonth}일 ($dow)"
    } catch (e: Exception) { date }

    private fun formatTimeShort(createdAt: String): String = try {
        val dt = LocalDateTime.parse(createdAt)
        val amPm = if (dt.hour < 12) "오전" else "오후"
        val hour = if (dt.hour % 12 == 0) 12 else dt.hour % 12
        "$amPm $hour:%02d".format(dt.minute)
    } catch (e: DateTimeParseException) { "" }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        const val EXTRA_DATE = "extra_date"
    }
}
