package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.repository.RecordFragmentRepository
import kotlinx.coroutines.launch

class AddMemoryFragment : Fragment() {

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

        // 날짜 텍스트 (요일 포함)
        val tvDate = view.findViewById<TextView>(R.id.tv_selected_date)
        val dateCal = java.util.Calendar.getInstance()
        dateCal.set(selectedYear, selectedMonth - 1, selectedDay)
        val dayOfWeekStr = arrayOf("일", "월", "화", "수", "목", "금", "토")[dateCal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        tvDate.text = "${selectedYear}년 ${selectedMonth}월 ${selectedDay}일 ${dayOfWeekStr}요일"

        // 현재 시간
        val tvTime = view.findViewById<TextView>(R.id.tv_time)
        val cal = java.util.Calendar.getInstance()
        val recordHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val recordMinute = cal.get(java.util.Calendar.MINUTE)
        tvTime.text = String.format("%02d:%02d", recordHour, recordMinute)

        // 오늘 추가된 기록 토글
        val layoutRecordsEmpty = view.findViewById<View>(R.id.layout_records_empty)
        val icToggle = view.findViewById<android.widget.ImageView>(R.id.ic_toggle_records)
        var isRecordsExpanded = false

        view.findViewById<View>(R.id.card_today_records).setOnClickListener {
            isRecordsExpanded = !isRecordsExpanded
            layoutRecordsEmpty.visibility = if (isRecordsExpanded) View.VISIBLE else View.GONE
            icToggle.rotation = if (isRecordsExpanded) 180f else 0f
        }

        // 뒤로 버튼
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 닫기 버튼 (홈으로)
        view.findViewById<View>(R.id.btn_close).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        // 기록 유형 버튼
        val btnPhoto = view.findViewById<LinearLayout>(R.id.btn_type_photo)
        val btnText = view.findViewById<LinearLayout>(R.id.btn_type_text)
        val btnVoice = view.findViewById<LinearLayout>(R.id.btn_type_voice)
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
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    private fun updateCardVisibility(cardPhoto: View, cardMemo: View, cardVoice: View) {
        cardPhoto.visibility = if (selectedType == RecordType.PHOTO) View.VISIBLE else View.GONE
        cardMemo.visibility = View.VISIBLE
        cardVoice.visibility = if (selectedType == RecordType.VOICE) View.VISIBLE else View.GONE
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
