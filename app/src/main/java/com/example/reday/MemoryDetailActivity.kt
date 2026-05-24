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
import com.example.reday.utils.toEmotionEmoji
import com.bumptech.glide.Glide
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

    private lateinit var flHero: FrameLayout
    private lateinit var ivHero: ImageView
    private lateinit var vHeroGradient: View
    private lateinit var llTitleOverlay: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvLocationDot: TextView
    private lateinit var ivLocationIcon: ImageView
    private lateinit var tvEmotion: TextView
    private lateinit var llHeaderNoImage: LinearLayout
    private lateinit var tvTitleNoImage: TextView
    private lateinit var tvDateNoImage: TextView
    private lateinit var tvLocationNoImage: TextView
    private lateinit var tvLocationDotNoImage: TextView
    private lateinit var ivLocationIconNoImage: ImageView
    private lateinit var tvEmotionNoImage: TextView
    private lateinit var tvSummary: TextView
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var tvFragmentCount: TextView
    private lateinit var llTimeline: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_detail)

        val date = intent.getStringExtra(EXTRA_DATE) ?: run { finish(); return }

        flHero = findViewById(R.id.fl_hero)
        ivHero = findViewById(R.id.iv_hero)
        vHeroGradient = findViewById(R.id.v_hero_gradient)
        llTitleOverlay = findViewById(R.id.ll_title_overlay)
        tvTitle = findViewById(R.id.tv_title)
        tvDate = findViewById(R.id.tv_date)
        tvLocation = findViewById(R.id.tv_location)
        tvLocationDot = findViewById(R.id.tv_location_dot)
        ivLocationIcon = findViewById(R.id.iv_location_icon)
        tvEmotion = findViewById(R.id.tv_emotion)
        llHeaderNoImage = findViewById(R.id.ll_header_no_image)
        tvTitleNoImage = findViewById(R.id.tv_title_no_image)
        tvDateNoImage = findViewById(R.id.tv_date_no_image)
        tvLocationNoImage = findViewById(R.id.tv_location_no_image)
        tvLocationDotNoImage = findViewById(R.id.tv_location_dot_no_image)
        ivLocationIconNoImage = findViewById(R.id.iv_location_icon_no_image)
        tvEmotionNoImage = findViewById(R.id.tv_emotion_no_image)
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
            val intent = android.content.Intent(this, MemoryFragmentActivity::class.java).apply {
                putExtra(MemoryFragmentActivity.EXTRA_DATE, date)
            }
            startActivity(intent)
        }

        dialog.findViewById<android.view.View>(R.id.btn_menu_delete_memory).setOnClickListener {
            dialog.dismiss()
            val serverId = intent.getLongExtra(EXTRA_SERVER_ID, 0L)
            showDeleteMemoryDialog(date, serverId)
        }

        dialog.show()
    }

    private fun showDeleteMemoryDialog(date: String, serverId: Long) {
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
                val fragments = fragmentRepository.getFragmentsByDate(date)
                fragments.forEach { fragmentRepository.deleteFragment(it) }
                if (serverId > 0L) {
                    memoryRepository.deleteMemoryById(serverId)
                } else {
                    memoryRepository.deleteMemoryByDate(date)
                }
                finish()
            }
        }
        dialog.show()
    }

    private fun loadData(date: String) {
        val serverId = intent.getLongExtra(EXTRA_SERVER_ID, 0L)
        lifecycleScope.launch {
            val entity = if (serverId > 0L) {
                val detail = memoryRepository.getMemoryDetail(serverId)
                if (detail != null) {
                    com.example.reday.data.local.entity.MemoryEntity(
                        serverId = null,
                        date = detail.memoryDate,
                        title = detail.title,
                        summary = detail.summary ?: "",
                        tags = com.google.gson.Gson().toJson(detail.tags.map { it.tagName }),
                        locations = com.google.gson.Gson().toJson(listOfNotNull(detail.location)),
                        people = "[]",
                        fragmentCount = 0,
                        representativeFragmentId = null,
                        representativePhotoUrl = detail.thumbnailUrl,
                        representativeLocationName = detail.location,
                        emotion = detail.emotion,
                        embedding = null,
                        createdAt = detail.memoryDate
                    )
                } else {
                    memoryRepository.getMemoryByDate(date) ?: run { finish(); return@launch }
                }
            } else {
                memoryRepository.getMemoryByDate(date) ?: run { finish(); return@launch }
            }

            // 히어로 사진 / 이미지 없을 때 헤더 분기
            val location = entity.representativeLocationName
                ?: parseLocations(entity.locations).firstOrNull()
            val emotionEmoji = entity.emotion.toEmotionEmoji()
            val dateLabel = formatDateLabel(date)

            if (!entity.representativePhotoUrl.isNullOrBlank()) {
                // 이미지 있음: 히어로 영역 표시
                val source: Any = if (entity.representativePhotoUrl!!.startsWith("http"))
                    entity.representativePhotoUrl!! else java.io.File(entity.representativePhotoUrl!!)
                Glide.with(this@MemoryDetailActivity).load(source).centerCrop().into(ivHero)
                flHero.layoutParams.height = (280 * resources.displayMetrics.density + 0.5f).toInt()
                flHero.requestLayout()
                ivHero.visibility = View.VISIBLE
                vHeroGradient.visibility = View.VISIBLE
                llTitleOverlay.visibility = View.VISIBLE
                llHeaderNoImage.visibility = View.GONE

                tvTitle.text = entity.title
                tvDate.text = dateLabel
                if (!location.isNullOrBlank()) {
                    tvLocation.text = location
                    tvLocation.visibility = View.VISIBLE
                    tvLocationDot.visibility = View.VISIBLE
                    ivLocationIcon.visibility = View.VISIBLE
                }
                if (!emotionEmoji.isNullOrBlank()) {
                    tvEmotion.text = emotionEmoji
                    tvEmotion.visibility = View.VISIBLE
                } else {
                    tvEmotion.visibility = View.GONE
                }
            } else {
                // 이미지 없음: 히어로를 툴바 높이로 축소, 별도 헤더 표시
                flHero.layoutParams.height = (64 * resources.displayMetrics.density + 0.5f).toInt()
                flHero.requestLayout()
                ivHero.visibility = View.GONE
                vHeroGradient.visibility = View.GONE
                llTitleOverlay.visibility = View.GONE
                llHeaderNoImage.visibility = View.VISIBLE

                // 버튼 색 변경 (밝은 배경이므로 어두운 색으로)
                val darkTint = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@MemoryDetailActivity, R.color.brown_700))
                ImageViewCompat.setImageTintList(findViewById(R.id.btn_back), darkTint)
                ImageViewCompat.setImageTintList(findViewById(R.id.btn_more), darkTint)

                tvTitleNoImage.text = entity.title
                tvDateNoImage.text = dateLabel
                if (!location.isNullOrBlank()) {
                    tvLocationNoImage.text = location
                    tvLocationNoImage.visibility = View.VISIBLE
                    tvLocationDotNoImage.visibility = View.VISIBLE
                    ivLocationIconNoImage.visibility = View.VISIBLE
                }
                if (!emotionEmoji.isNullOrBlank()) {
                    tvEmotionNoImage.text = emotionEmoji
                    tvEmotionNoImage.visibility = View.VISIBLE
                } else {
                    tvEmotionNoImage.visibility = View.GONE
                }
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
                    val source: Any = if (fragment.photoUrl!!.startsWith("http"))
                        fragment.photoUrl!! else java.io.File(fragment.photoUrl!!)
                    Glide.with(this@MemoryDetailActivity).load(source).centerCrop().into(imageView)
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
        const val EXTRA_SERVER_ID = "extra_server_id"
    }
}
