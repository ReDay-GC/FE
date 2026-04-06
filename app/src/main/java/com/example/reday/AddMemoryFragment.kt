package com.example.reday

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ProgressBar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.repository.RecordFragmentRepository
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AddMemoryFragment : Fragment() {

    interface AddMemoryListener {
        fun onBack()
        fun onSaved()
    }

    private var listener: AddMemoryListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? AddMemoryListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        private const val ARG_YEAR = "year"
        private const val ARG_MONTH = "month"
        private const val ARG_DAY = "day"

        fun newInstance(year: Int, month: Int, day: Int): AddMemoryFragment {
            return AddMemoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year)
                    putInt(ARG_MONTH, month)
                    putInt(ARG_DAY, day)
                }
            }
        }
    }

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0

    private var selectedType: RecordType = RecordType.TEXT
    private var selectedPhotoUri: Uri? = null

    private enum class VoiceUiState { IDLE, RECORDING, PAUSED, COMPLETED, PLAYING }
    private var voiceState = VoiceUiState.IDLE

    private var mediaRecorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var recordingTimer: CountDownTimer? = null
    private var elapsedSec = 0

    private var mediaPlayer: MediaPlayer? = null
    private val playbackHandler = Handler(Looper.getMainLooper())
    private val playbackRunnable: Runnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                val progress = if (mp.duration > 0) (mp.currentPosition * 100) / mp.duration else 0
                view?.findViewById<ProgressBar>(R.id.pb_playback)?.progress = progress
                playbackHandler.postDelayed(this, 100)
            }
        }
    }

    private lateinit var repository: RecordFragmentRepository

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else Toast.makeText(requireContext(), "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            view?.let { v ->
                v.findViewById<ImageView>(R.id.iv_photo_preview).apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
                v.findViewById<View>(R.id.layout_photo_placeholder).visibility = View.GONE
            }
        }
    }

    enum class RecordType { PHOTO, TEXT, VOICE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedYear = arguments?.getInt(ARG_YEAR) ?: 0
        selectedMonth = arguments?.getInt(ARG_MONTH) ?: 0
        selectedDay = arguments?.getInt(ARG_DAY) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_memory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        repository = RecordFragmentRepository(db.recordFragmentDao())

        // 현재 시간
        val tvTime = view.findViewById<TextView>(R.id.tv_time)
        val cal = java.util.Calendar.getInstance()
        val recordHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val recordMinute = cal.get(java.util.Calendar.MINUTE)
        tvTime.text = String.format("%02d:%02d", recordHour, recordMinute)

        // 오늘 추가된 기록 토글
        val layoutExpanded = view.findViewById<View>(R.id.layout_records_expanded)
        val icToggle = view.findViewById<ImageView>(R.id.ic_toggle_records)
        var isRecordsExpanded = false

        view.findViewById<View>(R.id.card_today_records).setOnClickListener {
            isRecordsExpanded = !isRecordsExpanded
            layoutExpanded.visibility = if (isRecordsExpanded) View.VISIBLE else View.GONE
            icToggle.rotation = if (isRecordsExpanded) 180f else 0f
        }

        // DB에서 기록 목록 실시간 구독
        val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getFragmentsByDate(date).collect { records ->
                updateRecordsList(view, records)
            }
        }

        // 기록 유형 버튼
        val btnPhoto = view.findViewById<LinearLayout>(R.id.btn_type_photo)
        val btnText = view.findViewById<LinearLayout>(R.id.btn_type_text)
        val btnVoice = view.findViewById<LinearLayout>(R.id.btn_type_voice)

        // 버튼을 정사각형으로 (너비 = 높이)
        listOf(btnPhoto, btnText, btnVoice).forEach { btn ->
            btn.post {
                btn.layoutParams = btn.layoutParams.also { it.height = btn.width }
            }
        }
        view.findViewById<View>(R.id.area_photo_upload).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<View>(R.id.fl_voice_icon).setOnClickListener {
            if (voiceState == VoiceUiState.IDLE) {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        view.findViewById<View>(R.id.btn_pause_resume).setOnClickListener {
            if (voiceState == VoiceUiState.RECORDING) pauseRecording()
            else if (voiceState == VoiceUiState.PAUSED) resumeRecording()
        }
        view.findViewById<View>(R.id.btn_record_done).setOnClickListener {
            completeRecording()
        }
        view.findViewById<View>(R.id.btn_play_pause_voice).setOnClickListener {
            if (voiceState == VoiceUiState.COMPLETED) startPlayback()
            else if (voiceState == VoiceUiState.PLAYING) pausePlayback()
        }
        view.findViewById<View>(R.id.tv_re_record).setOnClickListener {
            resetRecording()
        }

        val cardPhoto = view.findViewById<View>(R.id.card_photo)
        val cardMemo = view.findViewById<View>(R.id.card_memo)
        val cardVoice = view.findViewById<View>(R.id.card_voice)

        btnPhoto.setOnClickListener {
            selectedType = RecordType.PHOTO
            updateTypeButtons(btnPhoto, btnText, btnVoice)
            updateCardVisibility(cardPhoto, cardMemo, cardVoice)
        }

        btnText.setOnClickListener {
            selectedType = RecordType.TEXT
            updateTypeButtons(btnPhoto, btnText, btnVoice)
            updateCardVisibility(cardPhoto, cardMemo, cardVoice)
        }

        btnVoice.setOnClickListener {
            selectedType = RecordType.VOICE
            updateTypeButtons(btnPhoto, btnText, btnVoice)
            updateCardVisibility(cardPhoto, cardMemo, cardVoice)
        }

        // 초기 상태 적용 (텍스트 선택)
        updateTypeButtons(btnPhoto, btnText, btnVoice)
        updateCardVisibility(cardPhoto, cardMemo, cardVoice)

        // 저장 버튼
        val etMemo = view.findViewById<EditText>(R.id.et_memo)
        val etLocation = view.findViewById<EditText>(R.id.et_location)

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
            val createdAt = String.format(
                "%04d-%02d-%02dT%02d:%02d:00",
                selectedYear, selectedMonth, selectedDay, recordHour, recordMinute
            )
            val locationName = etLocation.text.toString().takeIf { it.isNotBlank() }

            lifecycleScope.launch {
                when (selectedType) {
                    RecordType.TEXT -> {
                        val text = etMemo.text.toString().trim()
                        if (text.isEmpty()) {
                            Toast.makeText(requireContext(), "메모를 입력해주세요", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        repository.saveTextFragment(text, createdAt, date, locationName)
                    }
                    RecordType.PHOTO -> {
                        val uri = selectedPhotoUri
                        if (uri == null) {
                            Toast.makeText(requireContext(), "사진을 선택해주세요", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val path = withContext(Dispatchers.IO) {
                            copyImageToInternalStorage(uri)
                        }
                        if (path == null) {
                            Toast.makeText(requireContext(), "사진 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val memo = etMemo.text.toString().trim().takeIf { it.isNotBlank() }
                        repository.savePhotoFragment(
                            photoUrl = path,
                            createdAt = createdAt,
                            date = date,
                            contentText = memo,
                            locationName = locationName
                        )
                    }
                    RecordType.VOICE -> {
                        if (voiceState != VoiceUiState.COMPLETED && voiceState != VoiceUiState.PLAYING) {
                            Toast.makeText(requireContext(), "먼저 녹음을 완료해주세요", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val file = voiceFile ?: return@launch
                        repository.saveVoiceFragment(
                            voiceUrl = file.absolutePath,
                            durationSec = elapsedSec,
                            date = date,
                            locationName = locationName
                        )
                    }
                }
                listener?.onSaved()
            }
        }
    }

    private fun startRecording() {
        val file = File(requireContext().filesDir, "voice_${System.currentTimeMillis()}.m4a")
        voiceFile = file
        elapsedSec = 0

        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            setMaxDuration(10_000)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    elapsedSec = 10
                    completeRecording()
                }
            }
            prepare()
            start()
        }
        voiceState = VoiceUiState.RECORDING
        updateVoiceUI(VoiceUiState.RECORDING)

        recordingTimer = object : CountDownTimer(10_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSec = ((10_000 - millisUntilFinished) / 1_000).toInt() + 1
                view?.findViewById<TextView>(R.id.tv_voice_timer)?.text =
                    String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)
            }
            override fun onFinish() {
                elapsedSec = 10
                completeRecording()
            }
        }.start()
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
            recordingTimer?.cancel()
            voiceState = VoiceUiState.PAUSED
            updateVoiceUI(VoiceUiState.PAUSED)
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
            val remaining = (10 - elapsedSec) * 1_000L
            recordingTimer = object : CountDownTimer(remaining, 1_000) {
                override fun onTick(millisUntilFinished: Long) {
                    elapsedSec++
                    view?.findViewById<TextView>(R.id.tv_voice_timer)?.text =
                        String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)
                }
                override fun onFinish() {
                    elapsedSec = 10
                    completeRecording()
                }
            }.start()
            voiceState = VoiceUiState.RECORDING
            updateVoiceUI(VoiceUiState.RECORDING)
        }
    }

    private fun completeRecording() {
        recordingTimer?.cancel()
        recordingTimer = null
        try {
            mediaRecorder?.apply { stop(); release() }
        } catch (e: Exception) {
            voiceFile?.delete()
            voiceFile = null
        } finally {
            mediaRecorder = null
        }
        voiceState = VoiceUiState.COMPLETED
        updateVoiceUI(VoiceUiState.COMPLETED)
    }

    private fun startPlayback() {
        val file = voiceFile ?: return
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener {
                playbackHandler.removeCallbacks(playbackRunnable)
                view?.findViewById<ProgressBar>(R.id.pb_playback)?.progress = 0
                voiceState = VoiceUiState.COMPLETED
                updateVoiceUI(VoiceUiState.COMPLETED)
            }
            start()
        }
        voiceState = VoiceUiState.PLAYING
        updateVoiceUI(VoiceUiState.PLAYING)
        playbackHandler.post(playbackRunnable)
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        playbackHandler.removeCallbacks(playbackRunnable)
        voiceState = VoiceUiState.COMPLETED
        updateVoiceUI(VoiceUiState.COMPLETED)
    }

    private fun resetRecording() {
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        playbackHandler.removeCallbacks(playbackRunnable)
        recordingTimer?.cancel()
        recordingTimer = null
        try { mediaRecorder?.apply { stop(); release() } } catch (e: Exception) {}
        mediaRecorder = null
        voiceFile?.delete()
        voiceFile = null
        elapsedSec = 0
        voiceState = VoiceUiState.IDLE
        updateVoiceUI(VoiceUiState.IDLE)
        view?.findViewById<TextView>(R.id.tv_voice_timer)?.text = "0:00"
    }

    private fun updateVoiceUI(state: VoiceUiState) {
        val v = view ?: return
        val tvWaveform = v.findViewById<View>(R.id.tv_waveform)
        val tvStatus = v.findViewById<TextView>(R.id.tv_voice_status)
        val tvTimer = v.findViewById<TextView>(R.id.tv_voice_timer)
        val layoutRecordControls = v.findViewById<View>(R.id.layout_record_controls)
        val btnPauseResume = v.findViewById<TextView>(R.id.btn_pause_resume)
        val layoutPlaybackControls = v.findViewById<View>(R.id.layout_playback_controls)
        val btnPlayPause = v.findViewById<ImageButton>(R.id.btn_play_pause_voice)
        val tvReRecord = v.findViewById<View>(R.id.tv_re_record)
        val tvHint = v.findViewById<View>(R.id.tv_voice_hint)

        when (state) {
            VoiceUiState.IDLE -> {
                tvWaveform.visibility = View.GONE
                tvStatus.text = "녹음 버튼을 눌러 시작하세요"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                tvTimer.visibility = View.GONE
                layoutRecordControls.visibility = View.GONE
                layoutPlaybackControls.visibility = View.GONE
                tvReRecord.visibility = View.GONE
                tvHint.visibility = View.VISIBLE
            }
            VoiceUiState.RECORDING -> {
                tvWaveform.visibility = View.VISIBLE
                tvStatus.text = "녹음 중..."
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.main_200))
                tvTimer.visibility = View.VISIBLE
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_800))
                layoutRecordControls.visibility = View.VISIBLE
                btnPauseResume.text = "일시정지"
                layoutPlaybackControls.visibility = View.GONE
                tvReRecord.visibility = View.GONE
                tvHint.visibility = View.GONE
            }
            VoiceUiState.PAUSED -> {
                tvWaveform.visibility = View.GONE
                tvStatus.text = "일시정됨"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                tvTimer.visibility = View.VISIBLE
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_800))
                layoutRecordControls.visibility = View.VISIBLE
                btnPauseResume.text = "재개"
                layoutPlaybackControls.visibility = View.GONE
                tvReRecord.visibility = View.GONE
                tvHint.visibility = View.GONE
            }
            VoiceUiState.COMPLETED -> {
                tvWaveform.visibility = View.GONE
                tvStatus.text = "녹음 완료!"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub_200))
                tvTimer.visibility = View.VISIBLE
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub_200))
                layoutRecordControls.visibility = View.GONE
                layoutPlaybackControls.visibility = View.VISIBLE
                btnPlayPause.setImageResource(R.drawable.ic_play)
                tvReRecord.visibility = View.VISIBLE
                tvHint.visibility = View.GONE
            }
            VoiceUiState.PLAYING -> {
                tvWaveform.visibility = View.GONE
                tvStatus.text = "재생 중..."
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                tvTimer.visibility = View.VISIBLE
                tvTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub_200))
                layoutRecordControls.visibility = View.GONE
                layoutPlaybackControls.visibility = View.VISIBLE
                btnPlayPause.setImageResource(R.drawable.ic_pause_bars)
                tvReRecord.visibility = View.VISIBLE
                tvHint.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (voiceState == VoiceUiState.RECORDING || voiceState == VoiceUiState.PAUSED) {
            recordingTimer?.cancel()
            try { mediaRecorder?.apply { stop(); release() } } catch (e: Exception) {}
            mediaRecorder = null
        }
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        playbackHandler.removeCallbacks(playbackRunnable)
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            file.outputStream().use { output -> input.copyTo(output) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun hasUnsavedContent(): Boolean {
        val etMemo = view?.findViewById<EditText>(R.id.et_memo)
        val etLocation = view?.findViewById<EditText>(R.id.et_location)
        return selectedPhotoUri != null ||
               voiceState == VoiceUiState.COMPLETED || voiceState == VoiceUiState.PLAYING ||
               etMemo?.text?.isNotBlank() == true ||
               etLocation?.text?.isNotBlank() == true
    }

    private fun updateCardVisibility(cardPhoto: View, cardMemo: View, cardVoice: View) {
        cardPhoto.visibility = if (selectedType == RecordType.PHOTO) View.VISIBLE else View.GONE
        cardMemo.visibility = View.VISIBLE
        cardVoice.visibility = if (selectedType == RecordType.VOICE) View.VISIBLE else View.GONE
    }

    private fun updateRecordsList(view: View, records: List<RecordFragmentUiModel>) {
        val tvCount = view.findViewById<TextView>(R.id.tv_record_count)
        val layoutList = view.findViewById<LinearLayout>(R.id.layout_records_list)
        val layoutEmpty = view.findViewById<View>(R.id.layout_records_empty)

        tvCount.text = "${records.size}개"
        layoutList.removeAllViews()

        if (records.isEmpty()) {
            layoutList.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            layoutList.visibility = View.VISIBLE
            records.forEach { record ->
                layoutList.addView(buildRecordItemView(record))
            }
        }
    }

    private fun buildRecordItemView(record: RecordFragmentUiModel): View {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_record_fragment, null)

        // 아이콘
        val ivIcon = itemView.findViewById<ImageView>(R.id.iv_type_icon)
        ivIcon.setImageResource(when (record.fragmentType) {
            FragmentType.PHOTO -> R.drawable.ic_camera
            FragmentType.TEXT  -> R.drawable.ic_text_type
            FragmentType.VOICE -> R.drawable.ic_mic
        })

        // 미리보기 텍스트
        val tvPreview = itemView.findViewById<TextView>(R.id.tv_preview)
        tvPreview.text = when (record.fragmentType) {
            FragmentType.TEXT  -> record.contentText ?: ""
            FragmentType.PHOTO -> "사진"
            FragmentType.VOICE -> "음성 녹음"
        }

        // 시간
        itemView.findViewById<TextView>(R.id.tv_time).text = formatTime(record.createdAt)

        // 상세 내용 — PHOTO: 이미지 표시 / TEXT·VOICE: 텍스트 표시
        val ivPhotoDetail = itemView.findViewById<ImageView>(R.id.iv_photo_detail)
        val tvFullContent = itemView.findViewById<TextView>(R.id.tv_full_content)

        if (record.fragmentType == FragmentType.PHOTO && record.photoUrl != null) {
            val bitmap = BitmapFactory.decodeFile(record.photoUrl)
            if (bitmap != null) {
                ivPhotoDetail.setImageBitmap(bitmap)
                ivPhotoDetail.visibility = View.VISIBLE
            } else {
                ivPhotoDetail.visibility = View.GONE
            }
            if (!record.contentText.isNullOrBlank()) {
                tvFullContent.text = record.contentText
                tvFullContent.visibility = View.VISIBLE
            } else {
                tvFullContent.visibility = View.GONE
            }
        } else if (record.fragmentType == FragmentType.VOICE) {
            ivPhotoDetail.visibility = View.GONE
            tvFullContent.visibility = View.GONE
            val layoutVoice = itemView.findViewById<View>(R.id.layout_voice_player)
            val tvDuration = itemView.findViewById<TextView>(R.id.tv_voice_duration)
            val sec = record.durationSec ?: 0
            tvDuration.text = String.format("%d:%02d", sec / 60, sec % 60)
            layoutVoice.visibility = View.VISIBLE
        } else {
            ivPhotoDetail.visibility = View.GONE
            itemView.findViewById<View>(R.id.layout_voice_player).visibility = View.GONE
            tvFullContent.visibility = View.VISIBLE
            tvFullContent.text = record.contentText ?: ""
        }

        // 위치
        val rowLocation = itemView.findViewById<View>(R.id.row_location)
        val tvLocation = itemView.findViewById<TextView>(R.id.tv_location)
        if (record.locationName != null) {
            tvLocation.text = record.locationName
            rowLocation.visibility = View.VISIBLE
        } else {
            rowLocation.visibility = View.GONE
        }

        itemView.findViewById<TextView>(R.id.tv_full_datetime).text =
            formatFullDatetime(record.createdAt)

        // 개별 아이템 펼치기/접기
        val layoutDetail = itemView.findViewById<View>(R.id.layout_detail)
        val ivToggle = itemView.findViewById<ImageView>(R.id.iv_toggle)
        var isExpanded = false

        itemView.findViewById<View>(R.id.row_summary).setOnClickListener {
            isExpanded = !isExpanded
            layoutDetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivToggle.rotation = if (isExpanded) 180f else 0f
        }

        return itemView
    }

    private fun formatTime(createdAt: String): String {
        val time = createdAt.split("T").getOrNull(1) ?: return ""
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return ""
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return ""
        val ampm = if (hour < 12) "오전" else "오후"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$ampm ${String.format("%02d:%02d", h, minute)}"
    }

    private fun formatFullDatetime(createdAt: String): String {
        val parts = createdAt.split("T")
        val datePart = parts.getOrNull(0) ?: return ""
        val datePieces = datePart.split("-")
        val year = datePieces.getOrNull(0) ?: return ""
        val month = datePieces.getOrNull(1)?.toIntOrNull() ?: return ""
        val day = datePieces.getOrNull(2)?.toIntOrNull() ?: return ""
        return "${year}년 ${month}월 ${day}일 ${formatTime(createdAt)}"
    }

    private fun updateTypeButtons(
        btnPhoto: LinearLayout,
        btnText: LinearLayout,
        btnVoice: LinearLayout
    ) {
        listOf(btnPhoto, btnText, btnVoice).forEachIndexed { index, btn ->
            val isSelected = when (index) {
                0 -> selectedType == RecordType.PHOTO
                1 -> selectedType == RecordType.TEXT
                2 -> selectedType == RecordType.VOICE
                else -> false
            }
            val textColor = ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.sub_200 else R.color.brown_500
            )
            val iconTint = android.content.res.ColorStateList.valueOf(textColor)

            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_record_type_btn_selected
                else R.drawable.bg_record_type_btn
            )
            // 아이콘 tint
            (btn.getChildAt(0) as? android.widget.ImageView)?.imageTintList = iconTint
            // 텍스트 색
            (btn.getChildAt(1) as? android.widget.TextView)?.setTextColor(textColor)
        }
    }
}
