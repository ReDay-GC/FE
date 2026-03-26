package com.example.reday

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.repository.RecordFragmentRepository
import kotlinx.coroutines.launch

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

    private lateinit var repository: RecordFragmentRepository

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
                        // 갤러리 연동 후 photoUrl 전달 예정
                        val memo = etMemo.text.toString().trim()
                        Toast.makeText(requireContext(), "사진 기능은 준비 중입니다", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    RecordType.VOICE -> {
                        // 녹음 연동 후 voiceUrl 전달 예정
                        Toast.makeText(requireContext(), "음성 기능은 준비 중입니다", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }
                listener?.onSaved()
            }
        }
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
            FragmentType.VOICE -> "음성 기록"
        }

        // 시간
        itemView.findViewById<TextView>(R.id.tv_time).text = formatTime(record.createdAt)

        // 상세 내용
        itemView.findViewById<TextView>(R.id.tv_full_content).text = when (record.fragmentType) {
            FragmentType.TEXT  -> record.contentText ?: ""
            FragmentType.PHOTO -> "사진 기록"
            FragmentType.VOICE -> "음성 기록"
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
