package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

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

    // 기록 유형 선택 상태 (기본: 텍스트)
    private var selectedType: RecordType = RecordType.TEXT

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

        // 날짜 텍스트 (요일 포함)
        val tvDate = view.findViewById<TextView>(R.id.tv_selected_date)
        val dateCal = java.util.Calendar.getInstance()
        dateCal.set(selectedYear, selectedMonth - 1, selectedDay)
        val dayOfWeekStr = arrayOf("일", "월", "화", "수", "목", "금", "토")[dateCal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        tvDate.text = "${selectedYear}년 ${selectedMonth}월 ${selectedDay}일 ${dayOfWeekStr}요일"

        // 현재 시간
        val tvTime = view.findViewById<TextView>(R.id.tv_time)
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        tvTime.text = String.format("%02d:%02d", hour, minute)

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
        val cardMemo = view.findViewById<View>(R.id.card_memo)

        btnPhoto.setOnClickListener {
            selectedType = RecordType.PHOTO
            updateTypeButtons(btnPhoto, btnText, btnVoice)
            cardMemo.visibility = View.GONE
        }

        btnText.setOnClickListener {
            selectedType = RecordType.TEXT
            updateTypeButtons(btnPhoto, btnText, btnVoice)
            cardMemo.visibility = View.VISIBLE
        }

        btnVoice.setOnClickListener {
            selectedType = RecordType.VOICE
            updateTypeButtons(btnPhoto, btnText, btnVoice)
            cardMemo.visibility = View.GONE
        }

        // 초기 상태 적용 (텍스트 선택 → 메모 카드 표시)
        cardMemo.visibility = View.VISIBLE
        updateTypeButtons(btnPhoto, btnText, btnVoice)

        // 저장 버튼
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            // 추후 저장 로직 연결
        }
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
            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_record_type_btn_selected
                else R.drawable.bg_record_type_btn
            )
        }
    }
}
