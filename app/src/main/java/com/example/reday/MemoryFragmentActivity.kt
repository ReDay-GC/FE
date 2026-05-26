package com.example.reday

import android.app.Dialog
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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

    private var mediaPlayer: MediaPlayer? = null
    private var playingFragmentId: Long? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var activePlayBtn: ImageView? = null
    private var activeProgressBar: ProgressBar? = null
    private var activeTimeText: TextView? = null

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
            lifecycleScope.launch {
                existingMemoryId = memoryRepository.getMemoryByDate(currentDate)?.serverId
                if (existingMemoryId != null) {
                    showOverwriteConfirmDialog()
                } else {
                    startAiGeneration()
                }
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
            val editBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(20.dp, 20.dp).also {
                    it.marginStart = 10.dp
                }
                setImageResource(R.drawable.ic_edit)
                ImageViewCompat.setImageTintList(this,
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_400)))
                setOnClickListener { showFragmentOptionsPopup(it, fragment) }
            }
            header.addView(editBtn)
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
                if (!fragment.contentText.isNullOrBlank()) {
                    content.addView(createContentText("\"${fragment.contentText}\""))
                }

                val duration = fragment.durationSec ?: 0
                val isThisPlaying = playingFragmentId == fragment.localId && mediaPlayer?.isPlaying == true
                val currentSec = if (playingFragmentId == fragment.localId) (mediaPlayer?.currentPosition ?: 0) / 1000 else 0

                val playerRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 4.dp }
                }

                val playBtn = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp).also { it.marginEnd = 8.dp }
                    setImageResource(if (isThisPlaying) R.drawable.ic_pause_bars else R.drawable.ic_play)
                    ImageViewCompat.setImageTintList(
                        this,
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this@MemoryFragmentActivity, R.color.main_200)
                        )
                    )
                }

                val progressBar = ProgressBar(
                    this, null, android.R.attr.progressBarStyleHorizontal
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        it.marginStart = 4.dp
                        it.marginEnd = 8.dp
                    }
                    max = if (duration > 0) duration else 1
                    progress = currentSec
                }

                val timeText = TextView(this).apply {
                    text = formatVoiceTime(currentSec, duration)
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(this@MemoryFragmentActivity, R.color.brown_400))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                if (playingFragmentId == fragment.localId) {
                    activePlayBtn = playBtn
                    activeProgressBar = progressBar
                    activeTimeText = timeText
                    if (isThisPlaying) startProgressUpdate(progressBar, timeText, duration)
                }

                playerRow.addView(playBtn)
                playerRow.addView(progressBar)
                playerRow.addView(timeText)
                content.addView(playerRow)

                playBtn.setOnClickListener {
                    toggleVoicePlayback(fragment, playBtn, progressBar, timeText)
                }
            }
        }

        return content
    }

    private fun toggleVoicePlayback(
        fragment: RecordFragmentUiModel,
        playBtn: ImageView,
        progressBar: ProgressBar,
        timeText: TextView
    ) {
        val voiceUrl = fragment.voiceUrl
        if (voiceUrl.isNullOrBlank()) {
            Toast.makeText(this, "음성 파일을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        if (playingFragmentId == fragment.localId) {
            val player = mediaPlayer ?: return
            if (player.isPlaying) {
                player.pause()
                playBtn.setImageResource(R.drawable.ic_play)
                stopProgressUpdate()
            } else {
                player.start()
                playBtn.setImageResource(R.drawable.ic_pause_bars)
                startProgressUpdate(progressBar, timeText, fragment.durationSec ?: 0)
            }
            ImageViewCompat.setImageTintList(
                playBtn,
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.main_200))
            )
        } else {
            stopVoice()
            playingFragmentId = fragment.localId
            activePlayBtn = playBtn
            activeProgressBar = progressBar
            activeTimeText = timeText

            val player = MediaPlayer()
            mediaPlayer = player
            val duration = fragment.durationSec ?: 0

            try {
                player.setDataSource(this, Uri.parse(voiceUrl))
                player.setOnPreparedListener { mp ->
                    mp.start()
                    playBtn.setImageResource(R.drawable.ic_pause_bars)
                    ImageViewCompat.setImageTintList(
                        playBtn,
                        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.main_200))
                    )
                    startProgressUpdate(progressBar, timeText, duration)
                }
                player.setOnCompletionListener {
                    playBtn.setImageResource(R.drawable.ic_play)
                    ImageViewCompat.setImageTintList(
                        playBtn,
                        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.main_200))
                    )
                    progressBar.progress = 0
                    timeText.text = formatVoiceTime(0, duration)
                    stopProgressUpdate()
                    playingFragmentId = null
                    mediaPlayer = null
                }
                player.prepareAsync()
            } catch (e: Exception) {
                Toast.makeText(this, "재생 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                player.release()
                mediaPlayer = null
                playingFragmentId = null
            }
        }
    }

    private fun stopVoice() {
        stopProgressUpdate()
        activePlayBtn?.setImageResource(R.drawable.ic_play)
        activePlayBtn = null
        activeProgressBar = null
        activeTimeText = null
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun startProgressUpdate(progressBar: ProgressBar, timeText: TextView, durationSec: Int) {
        stopProgressUpdate()
        progressRunnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return
                if (player.isPlaying) {
                    val currentSec = player.currentPosition / 1000
                    progressBar.progress = currentSec
                    timeText.text = formatVoiceTime(currentSec, durationSec)
                    progressHandler.postDelayed(this, 500)
                }
            }
        }
        progressHandler.post(progressRunnable!!)
    }

    private fun stopProgressUpdate() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun formatVoiceTime(currentSec: Int, totalSec: Int): String {
        fun fmt(s: Int) = "%d:%02d".format(s / 60, s % 60)
        return "${fmt(currentSec)} / ${fmt(totalSec)}"
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            activePlayBtn?.setImageResource(R.drawable.ic_play)
            stopProgressUpdate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVoice()
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

    private fun showFragmentOptionsPopup(anchor: View, fragment: RecordFragmentUiModel) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 0, 0, "수정")
        popup.menu.add(0, 1, 1, "삭제")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> { showEditDialog(fragment); true }
                1 -> { showDeleteConfirmDialog(fragment); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDialog(fragment: RecordFragmentUiModel) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_record)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialog.findViewById<android.widget.TextView>(R.id.tv_edit_title)
        val etContent = dialog.findViewById<android.widget.EditText>(R.id.et_edit_content)
        val etLocation = dialog.findViewById<android.widget.EditText>(R.id.et_edit_location)

        when (fragment.fragmentType) {
            FragmentType.PHOTO -> tvTitle.text = "사진 기록 수정"
            FragmentType.TEXT -> tvTitle.text = "텍스트 기록 수정"
            FragmentType.VOICE -> tvTitle.text = "음성 기록 수정"
        }

        if (fragment.fragmentType == FragmentType.VOICE) {
            etContent.hint = "텍스트 메모 (선택)"
        }

        etContent.setText(fragment.contentText ?: "")
        etLocation.setText(fragment.locationName ?: "")

        dialog.findViewById<android.widget.TextView>(R.id.btn_edit_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.widget.TextView>(R.id.btn_edit_confirm).setOnClickListener {
            val newContent = etContent.text.toString().trim().ifEmpty { null }
            val newLocation = etLocation.text.toString().trim().ifEmpty { null }
            dialog.dismiss()
            lifecycleScope.launch {
                try {
                    repository.updateFragment(
                        model = fragment,
                        textContent = newContent,
                        address = newLocation,
                        latitude = fragment.latitude,
                        longitude = fragment.longitude
                    )
                    loadFragments()
                } catch (e: Exception) {
                    Toast.makeText(this@MemoryFragmentActivity, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
