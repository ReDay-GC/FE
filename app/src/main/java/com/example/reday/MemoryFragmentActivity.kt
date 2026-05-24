package com.example.reday

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.remote.FragmentInput
import com.example.reday.data.remote.GenerateMemoryRequest
import com.example.reday.data.remote.GenerateMemoryResponse
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.data.repository.RecordFragmentRepository
import com.bumptech.glide.Glide
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.util.Locale
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class MemoryFragmentActivity : AppCompatActivity() {

    private lateinit var repository: RecordFragmentRepository
    private lateinit var memoryRepository: MemoryRepository
    private lateinit var llTimeline: LinearLayout
    private lateinit var tvToolbarDate: TextView
    private lateinit var tvBannerTitle: TextView

    private var currentDate: String = ""
    private var fragments: List<RecordFragmentUiModel> = emptyList()
    private val expandedStates = mutableMapOf<Long, Boolean>()
    private var existingMemoryId: Long? = null
    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_fragment)

        val date = intent.getStringExtra(EXTRA_DATE) ?: run { finish(); return }
        currentDate = date

        tvToolbarDate = findViewById(R.id.tv_toolbar_date)
        tvBannerTitle = findViewById(R.id.tv_banner_title)
        llTimeline = findViewById(R.id.ll_timeline)

        tvToolbarDate.text = formatDateLabel(date)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_generate_memory).setOnClickListener {
            if (existingMemoryId != null) {
                showOverwriteConfirmDialog()
            } else {
                startAiGeneration()
            }
        }
        findViewById<View>(R.id.btn_add_fragment).setOnClickListener {
            val parts = date.split("-")
            val intent = android.content.Intent(this, AddMemoryActivity::class.java).apply {
                putExtra(AddMemoryActivity.EXTRA_YEAR, parts[0].toInt())
                putExtra(AddMemoryActivity.EXTRA_MONTH, parts[1].toInt())
                putExtra(AddMemoryActivity.EXTRA_DAY, parts[2].toInt())
                putExtra(AddMemoryActivity.EXTRA_GO_TO_TIMELINE, true)
            }
            startActivity(intent)
        }

        repository = RecordFragmentRepository()
        memoryRepository = MemoryRepository()
    }

    override fun onResume() {
        super.onResume()
        isGenerating = false
        lifecycleScope.launch {
            existingMemoryId = memoryRepository.getMemoryByDate(currentDate)?.serverId
        }
        loadFragments()
    }

    private fun loadFragments() {
        lifecycleScope.launch {
            val frags = repository.getFragmentsByDate(currentDate)
            fragments = frags
            tvBannerTitle.text = "${frags.size}개의 기억 조각이 있어요"
            buildTimeline()
        }
    }

    private fun buildTimeline() {
        llTimeline.removeAllViews()
        val fragmentsByHour = fragments.groupBy { getHour(it.createdAt) }
        for (hour in 0..23) {
            llTimeline.addView(createHourRow(hour, fragmentsByHour[hour] ?: emptyList()))
        }
    }

    private fun createHourRow(hour: Int, hourFragments: List<RecordFragmentUiModel>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 시간 라벨
        val timeLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.MATCH_PARENT)
            text = "%02d:00".format(hour)
            textSize = 11f
            gravity = Gravity.TOP or Gravity.END
            setPadding(0, 10.dp, 6.dp, 0)
            setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity,
                if (hourFragments.isNotEmpty()) R.color.brown_600 else R.color.brown_300))
        }
        row.addView(timeLabel)

        // 타임라인 선 + 인디케이터
        row.addView(createTimelineIndicator(hourFragments))

        // 콘텐츠 영역
        val contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = 10.dp
            }
            minimumHeight = 36.dp
        }

        hourFragments.forEach { fragment ->
            contentArea.addView(createFragmentItem(fragment))
        }

        row.addView(contentArea)
        return row
    }

    private fun createTimelineIndicator(hourFragments: List<RecordFragmentUiModel>): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(20.dp, LinearLayout.LayoutParams.MATCH_PARENT)

            // 수직 선
            addView(View(this@MemoryFragmentActivity).apply {
                layoutParams = FrameLayout.LayoutParams(1.dp, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
                setBackgroundColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_200))
            })

            if (hourFragments.isNotEmpty()) {
                val firstType = hourFragments.first().fragmentType
                addView(ImageView(this@MemoryFragmentActivity).apply {
                    val size = 20.dp
                    layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.CENTER_HORIZONTAL).also {
                        it.topMargin = 6.dp
                    }
                    when (firstType) {
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
                                    ContextCompat.getColor(this@MemoryFragmentActivity, R.color.main_200)))
                        }
                    }
                    setPadding(4.dp, 4.dp, 4.dp, 4.dp)
                })
            } else {
                // 빈 점
                addView(View(this@MemoryFragmentActivity).apply {
                    val size = 5.dp
                    layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.CENTER_HORIZONTAL).also {
                        it.topMargin = 14.dp
                    }
                    background = ContextCompat.getDrawable(this@MemoryFragmentActivity, R.drawable.bg_dot_main200)
                    alpha = 0.3f
                })
            }
        }
    }

    private fun createFragmentItem(fragment: RecordFragmentUiModel): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 2.dp }
        }

        val isExpanded = expandedStates[fragment.localId] ?: false

        // 헤더: 타입 + 시간 + 화살표
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                36.dp
            )
        }

        val typeLabel = TextView(this).apply {
            text = when (fragment.fragmentType) {
                FragmentType.PHOTO -> "사진"
                FragmentType.TEXT -> "텍스트"
                FragmentType.VOICE -> "음성 메모"
            }
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_700))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(typeLabel)

        val timeLabel = TextView(this).apply {
            text = formatTimeShort(fragment.createdAt)
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_400))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = 6.dp }
        }
        header.addView(timeLabel)

        val chevron = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp)
            setImageResource(R.drawable.ic_chevron_down)
            rotation = if (isExpanded) 180f else 0f
            ImageViewCompat.setImageTintList(this,
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_400)))
        }
        header.addView(chevron)

        if (isExpanded) {
            val deleteBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(20.dp, 20.dp).also {
                    it.marginStart = 10.dp
                }
                setImageResource(R.drawable.ic_delete)
                setOnClickListener { showDeleteConfirmDialog(fragment) }
            }
            header.addView(deleteBtn)
        }

        container.addView(header)

        // 펼쳐진 콘텐츠
        if (isExpanded) {
            container.addView(createExpandedContent(fragment))
        }

        header.setOnClickListener {
            expandedStates[fragment.localId] = !isExpanded
            buildTimeline()
        }

        return container
    }

    private fun createExpandedContent(fragment: RecordFragmentUiModel): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 6.dp }
            setPadding(0, 4.dp, 0, 0)
        }

        when (fragment.fragmentType) {
            FragmentType.PHOTO -> {
                if (!fragment.photoUrl.isNullOrBlank()) {
                    val imageView = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.bottomMargin = 6.dp }
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        setBackgroundResource(R.drawable.bg_photo_preview_rounded)
                        clipToOutline = true
                    }
                    val source: Any = if (fragment.photoUrl!!.startsWith("http"))
                        fragment.photoUrl!! else java.io.File(fragment.photoUrl!!)
                    Glide.with(this@MemoryFragmentActivity).load(source).centerCrop().into(imageView)
                    content.addView(imageView)
                }
                if (!fragment.locationName.isNullOrBlank()) {
                    content.addView(createLocationText(fragment.locationName!!))
                }
                if (!fragment.contentText.isNullOrBlank()) {
                    content.addView(createContentText("\"${fragment.contentText}\""))
                }
            }
            FragmentType.TEXT -> {
                if (!fragment.locationName.isNullOrBlank()) {
                    content.addView(createLocationText(fragment.locationName!!))
                }
                if (!fragment.contentText.isNullOrBlank()) {
                    content.addView(createContentText("\"${fragment.contentText}\""))
                }
            }
            FragmentType.VOICE -> {
                if (!fragment.locationName.isNullOrBlank()) {
                    content.addView(createLocationText(fragment.locationName!!))
                }
                val duration = fragment.durationSec ?: 0
                content.addView(TextView(this).apply {
                    text = formatDuration(duration)
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_500))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 4.dp }
                })
            }
        }

        return content
    }

    private fun createLocationText(location: String): TextView {
        return TextView(this).apply {
            text = "@ $location"
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_400))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4.dp }
        }
    }

    private fun createContentText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_600))
            setLineSpacing(0f, 1.4f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4.dp }
        }
    }

    private fun getHour(createdAt: String): Int = try {
        LocalDateTime.parse(createdAt).hour
    } catch (e: DateTimeParseException) { 0 }

    private fun formatTimeShort(createdAt: String): String = try {
        val dt = LocalDateTime.parse(createdAt)
        "%02d:%02d".format(dt.hour, dt.minute)
    } catch (e: DateTimeParseException) { "" }

    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "0:%02d / %02d:%02d".format(0, min, sec)
    }

    private fun startAiGeneration() {
        if (isGenerating) return
        if (fragments.isEmpty()) {
            Toast.makeText(this, "기록 조각이 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        isGenerating = true

        val loadingDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_ai_loading)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val photoData = fragments
                    .filter { it.fragmentType == FragmentType.PHOTO && it.photoUrl != null }
                    .mapNotNull { f ->
                        try {
                            val url = f.photoUrl!!
                            val bytes = withContext(Dispatchers.IO) {
                                if (url.startsWith("http")) {
                                    java.net.URL(url).openStream().use { it.readBytes() }
                                } else {
                                    java.io.File(url).readBytes()
                                }
                            }
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        } catch (e: Exception) { null }
                    }

                val request = GenerateMemoryRequest(
                    date = currentDate,
                    records = fragments.map { f ->
                        FragmentInput(
                            type = f.fragmentType.name,
                            content = f.contentText ?: "",
                            time = f.createdAt,
                            location = f.locationName
                        )
                    },
                    photo_data = photoData
                )
                val response = RetrofitClient.memoryApi.generateMemory(request)
                loadingDialog.dismiss()
                showCompleteDialog(response)
            } catch (e: Exception) {
                loadingDialog.dismiss()
                isGenerating = false
                android.util.Log.e("MemoryAI", "AI 생성 오류: ${e.javaClass.simpleName}: ${e.message}", e)
                Toast.makeText(this@MemoryFragmentActivity, "오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCompleteDialog(response: GenerateMemoryResponse) {
        val completeDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_ai_complete)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCanceledOnTouchOutside(true)
        }
        completeDialog.setOnDismissListener {
            lifecycleScope.launch {
                val geocoder = Geocoder(this@MemoryFragmentActivity, Locale.KOREA)
                val locations = withContext(Dispatchers.IO) {
                    fragments.mapNotNull { fragment ->
                        val lat = fragment.latitude
                        val lng = fragment.longitude
                        if (lat != null && lng != null) {
                            try {
                                val address = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                                address?.thoroughfare
                                    ?: address?.subLocality
                                    ?: address?.subAdminArea
                                    ?: fragment.locationName
                            } catch (e: Exception) {
                                fragment.locationName
                            }
                        } else {
                            fragment.locationName
                        }
                    }.filterNotNull().distinct()
                }
                val recordIds = fragments.mapNotNull { it.serverId }
                val intent = android.content.Intent(this@MemoryFragmentActivity, MemoryResultActivity::class.java).apply {
                    putExtra(MemoryResultActivity.EXTRA_DATE, currentDate)
                    putExtra(MemoryResultActivity.EXTRA_TITLE, response.title)
                    putExtra(MemoryResultActivity.EXTRA_SUMMARY, response.summary)
                    putStringArrayListExtra(MemoryResultActivity.EXTRA_TAGS, ArrayList(response.tags))
                    putStringArrayListExtra(MemoryResultActivity.EXTRA_LOCATIONS, ArrayList(locations))
                    putStringArrayListExtra(MemoryResultActivity.EXTRA_PEOPLE, ArrayList(response.people))
                    putExtra(MemoryResultActivity.EXTRA_FRAGMENT_COUNT, fragments.size)
                    putExtra(MemoryResultActivity.EXTRA_EMOTION, response.emotion)
                    putExtra(MemoryResultActivity.EXTRA_RECORD_IDS, LongArray(recordIds.size) { recordIds[it] })
                    if (response.embedding.isNotEmpty()) {
                        putExtra(MemoryResultActivity.EXTRA_EMBEDDING, com.google.gson.Gson().toJson(response.embedding))
                    }
                    existingMemoryId?.let { putExtra(MemoryResultActivity.EXTRA_EXISTING_MEMORY_ID, it) }
                }
                startActivity(intent)
            }
        }
        completeDialog.show()
    }

    private fun showOverwriteConfirmDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_confirm)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_title).text = "기억 다시 생성"
        dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_message).text =
            "이미 이 날짜의 기억이 있어요.\n새로 생성하면 기존 기억이 삭제됩니다.\n계속하시겠습니까?"
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_confirm).text = "다시 생성"
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_confirm).setOnClickListener {
            dialog.dismiss()
            startAiGeneration()
        }
        dialog.show()
    }

    private fun showDeleteConfirmDialog(fragment: RecordFragmentUiModel) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_confirm)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.widget.TextView>(R.id.btn_dialog_confirm).setOnClickListener {
            dialog.dismiss()
            lifecycleScope.launch {
                repository.deleteFragment(fragment)
                loadFragments()
            }
        }
        dialog.show()
    }

    private fun formatDateLabel(date: String): String = try {
        val ld = LocalDate.parse(date)
        val dow = when (ld.dayOfWeek) {
            DayOfWeek.MONDAY -> "월요일"
            DayOfWeek.TUESDAY -> "화요일"
            DayOfWeek.WEDNESDAY -> "수요일"
            DayOfWeek.THURSDAY -> "목요일"
            DayOfWeek.FRIDAY -> "금요일"
            DayOfWeek.SATURDAY -> "토요일"
            DayOfWeek.SUNDAY -> "일요일"
            else -> ""
        }
        "${ld.year}년 ${ld.monthValue}월 ${ld.dayOfMonth}일 $dow"
    } catch (e: Exception) { date }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        const val EXTRA_DATE = "extra_date"
    }
}
