package com.example.reday

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.CheckBox
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
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.repository.RecordFragmentRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.graphics.BitmapFactory
import android.view.Gravity
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import android.content.ContentUris
import android.provider.MediaStore
import com.bumptech.glide.Glide
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import java.util.Locale

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
    private var selectedPhotoPath: String? = null

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

    private var isSaving = false

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var recordHour = 0
    private var recordMinute = 0

    private var pendingExifUri: Uri? = null

    private val requestMediaLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingExifUri ?: return@registerForActivityResult
        pendingExifUri = null
        lifecycleScope.launch(Dispatchers.IO) {
            readExifFromUri(uri)
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchCurrentLocation()
        else {
            view?.findViewById<CheckBox>(R.id.cb_current_location)?.isChecked = false
            Toast.makeText(requireContext(), "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

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
            val ivPreview = view?.findViewById<ImageView>(R.id.iv_photo_preview)
            ivPreview?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.layout_photo_placeholder)?.visibility = View.GONE
            // 선택 즉시 내부 저장소로 복사 + 방향 보정 후 저장 + 원본 URI에서 EXIF 읽기
            lifecycleScope.launch(Dispatchers.IO) {
                val path = copyImageToInternalStorage(uri)
                if (path != null) {
                    // EXIF 회전을 픽셀에 적용해 파일 덮어쓰기 → 서버 업로드 시에도 올바른 방향
                    val bitmap = loadBitmapWithCorrectOrientation(path)
                    if (bitmap != null) {
                        saveOrientationCorrected(path, bitmap)
                    }
                    selectedPhotoPath = path
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) ivPreview?.setImageBitmap(bitmap)
                    }
                } else {
                    selectedPhotoPath = null
                }

                val hasMediaLocation = ContextCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasMediaLocation) {
                    withContext(Dispatchers.Main) {
                        pendingExifUri = uri
                        requestMediaLocationPermissionLauncher.launch(
                            android.Manifest.permission.ACCESS_MEDIA_LOCATION
                        )
                    }
                } else {
                    readExifFromUri(uri)
                }
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

        repository = RecordFragmentRepository()

        // 현재 위치 체크박스
        view.findViewById<CheckBox>(R.id.cb_current_location).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fetchCurrentLocation()
                } else {
                    requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                currentLatitude = null
                currentLongitude = null
                view.findViewById<EditText>(R.id.et_location).setText("")
            }
        }

        // 위치 직접 입력 시 Geocoder로 좌표 변환
        view.findViewById<EditText>(R.id.et_location).setOnFocusChangeListener { v, hasFocus ->
            val cb = view.findViewById<CheckBox>(R.id.cb_current_location)
            if (!hasFocus && !cb.isChecked) {
                val text = (v as EditText).text.toString()
                geocodeManualLocation(text)
            }
        }

        // 현재 시간 (클래스 변수 초기화 - EXIF로 나중에 덮어쓸 수 있음)
        val tvTime = view.findViewById<TextView>(R.id.tv_time_input)
        val cal = java.util.Calendar.getInstance()
        recordHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        recordMinute = cal.get(java.util.Calendar.MINUTE)
        tvTime.text = String.format("%02d:%02d", recordHour, recordMinute)

        // 시간 필드 탭 → NumberPicker 다이얼로그
        view.findViewById<View>(R.id.layout_time_input).setOnClickListener {
            val dialogView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(64, 48, 64, 16)
            }
            val hourPicker = NumberPicker(requireContext()).apply {
                minValue = 0; maxValue = 23; value = recordHour
                setFormatter { String.format("%02d", it) }
            }
            val colonView = TextView(requireContext()).apply {
                text = ":"; textSize = 24f
                setPadding(24, 0, 24, 0)
                gravity = Gravity.CENTER
            }
            val minutePicker = NumberPicker(requireContext()).apply {
                minValue = 0; maxValue = 59; value = recordMinute
                setFormatter { String.format("%02d", it) }
            }
            dialogView.addView(hourPicker)
            dialogView.addView(colonView)
            dialogView.addView(minutePicker)

            AlertDialog.Builder(requireContext())
                .setTitle("시간 선택")
                .setView(dialogView)
                .setPositiveButton("확인") { _, _ ->
                    recordHour = hourPicker.value
                    recordMinute = minutePicker.value
                    tvTime.text = String.format("%02d:%02d", recordHour, recordMinute)
                    view.findViewById<CheckBox>(R.id.cb_current_time)?.isChecked = false
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 현재 시간으로 기록 체크박스
        view.findViewById<CheckBox>(R.id.cb_current_time).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val now = java.util.Calendar.getInstance()
                recordHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                recordMinute = now.get(java.util.Calendar.MINUTE)
                tvTime.text = String.format("%02d:%02d", recordHour, recordMinute)
            }
        }

        // 오늘 추가된 기록 토글
        val layoutExpanded = view.findViewById<View>(R.id.layout_records_expanded)
        val icToggle = view.findViewById<ImageView>(R.id.ic_toggle_records)
        var isRecordsExpanded = false

        view.findViewById<View>(R.id.card_today_records).setOnClickListener {
            isRecordsExpanded = !isRecordsExpanded
            layoutExpanded.visibility = if (isRecordsExpanded) View.VISIBLE else View.GONE
            icToggle.rotation = if (isRecordsExpanded) 180f else 0f
        }

        // 서버에서 기록 목록 조회
        val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
        viewLifecycleOwner.lifecycleScope.launch {
            val records = repository.getFragmentsByDate(date)
            updateRecordsList(view, records)
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
            if (isSaving) return@setOnClickListener
            isSaving = true

            val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
            val createdAt = String.format(
                "%04d-%02d-%02dT%02d:%02d:00",
                selectedYear, selectedMonth, selectedDay, recordHour, recordMinute
            )
            val locationName = etLocation.text.toString().takeIf { it.isNotBlank() }

            lifecycleScope.launch {
                // 위치명은 있는데 좌표가 없으면 저장 전에 Geocoder로 변환
                if (locationName != null && currentLatitude == null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(requireContext(), Locale.KOREA)
                            val result = geocoder.getFromLocationName(locationName, 1)?.firstOrNull()
                            currentLatitude = result?.latitude
                            currentLongitude = result?.longitude
                        } catch (e: Exception) { /* 변환 실패 시 null 유지 */ }
                    }
                }

                when (selectedType) {
                    RecordType.TEXT -> {
                        val text = etMemo.text.toString().trim()
                        if (text.isEmpty()) {
                            Toast.makeText(requireContext(), "메모를 입력해주세요", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            return@launch
                        }
                        repository.saveTextFragment(
                            text, createdAt, date, locationName,
                            latitude = currentLatitude, longitude = currentLongitude
                        )
                    }
                    RecordType.PHOTO -> {
                        if (selectedPhotoUri == null) {
                            Toast.makeText(requireContext(), "사진을 선택해주세요", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            return@launch
                        }
                        // 선택 시점에 이미 복사됨. 아직 복사 중이면 재시도
                        val path = selectedPhotoPath ?: withContext(Dispatchers.IO) {
                            copyImageToInternalStorage(selectedPhotoUri!!)
                        }
                        if (path == null) {
                            Toast.makeText(requireContext(), "사진 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            return@launch
                        }
                        val memo = etMemo.text.toString().trim().takeIf { it.isNotBlank() }
                        repository.savePhotoFragment(
                            photoUrl = path,
                            createdAt = createdAt,
                            date = date,
                            contentText = memo,
                            locationName = locationName,
                            latitude = currentLatitude,
                            longitude = currentLongitude
                        )
                    }
                    RecordType.VOICE -> {
                        if (voiceState != VoiceUiState.COMPLETED && voiceState != VoiceUiState.PLAYING) {
                            Toast.makeText(requireContext(), "먼저 녹음을 완료해주세요", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            return@launch
                        }
                        val file = voiceFile ?: run { isSaving = false; return@launch }
                        val sttText = view?.findViewById<EditText>(R.id.et_stt_result)?.text?.toString()?.trim()
                        repository.saveVoiceFragment(
                            voiceUrl = file.absolutePath,
                            durationSec = elapsedSec,
                            date = date,
                            contentText = sttText?.ifEmpty { null },
                            locationName = locationName,
                            latitude = currentLatitude,
                            longitude = currentLongitude
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
            // stop() 예외가 나도 voiceFile은 유지 — STT 요청은 계속 시도
        } finally {
            mediaRecorder = null
        }
        voiceState = VoiceUiState.COMPLETED
        updateVoiceUI(VoiceUiState.COMPLETED)
        requestStt()
    }

    private fun requestStt() {
        val file = voiceFile ?: return
        val layoutStt = view?.findViewById<View>(R.id.layout_stt_result) ?: return
        val etStt = view?.findViewById<EditText>(R.id.et_stt_result) ?: return

        layoutStt.visibility = View.VISIBLE
        etStt.hint = "변환 중..."
        etStt.setText("")

        lifecycleScope.launch {
            try {
                Log.d("STT", "파일 경로: ${file.absolutePath}, 존재: ${file.exists()}, 크기: ${file.length()}")
                val requestBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.memoryApi.transcribe(part)
                }
                Log.d("STT", "변환 성공: ${response.text}")
                etStt.setText(response.text)
                etStt.hint = ""
            } catch (e: Exception) {
                Log.e("STT", "변환 실패: ${e.javaClass.simpleName} - ${e.message}", e)
                etStt.hint = "변환 실패. 직접 입력해주세요."
            }
        }
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

    // EXIF 회전을 픽셀에 적용한 뒤 같은 경로에 덮어씀
    // 이후 서버 업로드 시에도 이미 올바른 방향의 이미지가 전송됨
    private fun saveOrientationCorrected(path: String, bitmap: android.graphics.Bitmap) {
        try {
            File(path).outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            androidx.exifinterface.media.ExifInterface(path).apply {
                setAttribute(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
        } catch (e: Exception) { /* 실패해도 원본 파일은 유지됨 */ }
    }

    private fun fetchCurrentLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        lifecycleScope.launch(Dispatchers.IO) {
                            val geocoder = Geocoder(requireContext(), Locale.KOREA)
                            val address = try {
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    ?.firstOrNull()
                            } catch (e: Exception) { null }
                            val addressText = address?.let {
                                it.getAddressLine(0)
                                    ?.removePrefix("대한민국 ")
                                    ?: "${location.latitude}, ${location.longitude}"
                            } ?: "${location.latitude}, ${location.longitude}"
                            withContext(Dispatchers.Main) {
                                view?.findViewById<EditText>(R.id.et_location)?.setText(addressText)
                            }
                        }
                    } else {
                        view?.findViewById<CheckBox>(R.id.cb_current_location)?.isChecked = false
                        Toast.makeText(requireContext(), "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    view?.findViewById<CheckBox>(R.id.cb_current_location)?.isChecked = false
                    Toast.makeText(requireContext(), "위치 오류: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            view?.findViewById<CheckBox>(R.id.cb_current_location)?.isChecked = false
        }
    }

    private suspend fun readExifFromUri(uri: Uri) {
        try {
            val exif = createExifInterface(uri) ?: return

            val parsedTime = parseExifDatetime(exif.getAttribute(ExifInterface.TAG_DATETIME))
            val latLong = FloatArray(2)
            val hasGps = exif.getLatLong(latLong)

            withContext(Dispatchers.Main) {
                val v = view ?: return@withContext

                if (parsedTime != null) {
                    recordHour = parsedTime.first
                    recordMinute = parsedTime.second
                    v.findViewById<TextView>(R.id.tv_time_input)?.text =
                        String.format("%02d:%02d", recordHour, recordMinute)
                    v.findViewById<CheckBox>(R.id.cb_current_time)?.isChecked = false
                }

                if (hasGps) {
                    currentLatitude = latLong[0].toDouble()
                    currentLongitude = latLong[1].toDouble()
                    v.findViewById<CheckBox>(R.id.cb_current_location)?.isChecked = false

                    withContext(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(requireContext(), Locale.KOREA)
                            val address = geocoder.getFromLocation(
                                latLong[0].toDouble(), latLong[1].toDouble(), 1
                            )?.firstOrNull()
                            val addressText = address?.getAddressLine(0)
                                ?.removePrefix("대한민국 ")
                                ?: "${latLong[0]}, ${latLong[1]}"
                            withContext(Dispatchers.Main) {
                                view?.findViewById<EditText>(R.id.et_location)?.setText(addressText)
                            }
                        } catch (e: Exception) { }
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun createExifInterface(uri: Uri): ExifInterface? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return requireContext().contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        }

        val hasMediaLocation = ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_MEDIA_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val mediaId = uri.lastPathSegment?.toLongOrNull()
        if (mediaId != null) {
            val mediaUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)

            // 1. DATA 컬럼으로 실제 파일 경로 직접 접근
            try {
                requireContext().contentResolver.query(
                    mediaUri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val path = cursor.getString(0)
                        if (!path.isNullOrEmpty()) return ExifInterface(path)
                    }
                }
            } catch (e: Exception) { }

            // 2. FileDescriptor + setRequireOriginal
            try {
                val originalUri = MediaStore.setRequireOriginal(mediaUri)
                requireContext().contentResolver.openFileDescriptor(originalUri, "r")?.use { pfd ->
                    return ExifInterface(pfd.fileDescriptor)
                }
            } catch (e: Exception) { }

            // 3. InputStream + setRequireOriginal
            try {
                val originalUri = MediaStore.setRequireOriginal(mediaUri)
                requireContext().contentResolver.openInputStream(originalUri)?.use { stream ->
                    return ExifInterface(stream)
                }
            } catch (e: Exception) { }
        }

        return requireContext().contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
    }

    private fun parseExifDatetime(datetime: String?): Pair<Int, Int>? {
        // EXIF 형식: "2026:03:08 14:30:00"
        return try {
            val timePart = datetime?.split(" ")?.getOrNull(1) ?: return null
            val parts = timePart.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            null
        }
    }

    private fun geocodeManualLocation(locationText: String) {
        if (locationText.isBlank()) {
            currentLatitude = null
            currentLongitude = null
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.KOREA)
                val result = geocoder.getFromLocationName(locationText, 1)?.firstOrNull()
                withContext(Dispatchers.Main) {
                    currentLatitude = result?.latitude
                    currentLongitude = result?.longitude
                }
            } catch (e: Exception) {
                // 변환 실패 시 좌표 null 유지 (위치명은 저장됨)
            }
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
            val url = record.photoUrl!!
            ivPhotoDetail.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapWithCorrectOrientation(url)
                }
                if (bitmap != null) ivPhotoDetail.setImageBitmap(bitmap)
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
