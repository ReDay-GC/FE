package com.example.reday

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Calendar

class DateSelectFragment : Fragment() {

    interface DateSelectListener {
        fun onDateSelected(year: Int, month: Int, day: Int)
    }

    private var listener: DateSelectListener? = null

    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDay = -1

    // 더미데이터 (추후 repository에서 공급 예정) - key: Pair(year, month 0-based)
    private val hasRecordMap = mapOf(
        Pair(2026, 2) to setOf(3, 5, 8),
        Pair(2026, 3) to setOf(1, 7, 15),
        Pair(2026, 4) to setOf(5, 20)
    )
    private val recordingMap = mapOf(
        Pair(2026, 2) to setOf(10),
        Pair(2026, 3) to setOf(22),
        Pair(2026, 4) to setOf(3)
    )

    private lateinit var tvMonthYear: TextView
    private lateinit var gridCalendar: LinearLayout

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? DateSelectListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_date_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvMonthYear = view.findViewById(R.id.tv_month_year)
        gridCalendar = view.findViewById(R.id.grid_calendar)

        view.findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener {
            if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
            renderCalendar()
        }

        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener {
            if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
            renderCalendar()
        }

        renderCalendar()
    }

    private fun renderCalendar() {
        tvMonthYear.text = "${currentYear}년 ${currentMonth + 1}월"
        gridCalendar.removeAllViews()

        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)
        val isCurrentMonth = currentYear == todayYear && currentMonth == todayMonth
        val isFutureMonth = currentYear > todayYear
                || (currentYear == todayYear && currentMonth > todayMonth)
        val todayDay = if (isCurrentMonth) todayDayOfMonth else -1

        val key = Pair(currentYear, currentMonth)
        val hasRecordDays = hasRecordMap[key] ?: emptySet()
        val recordingDays = recordingMap[key] ?: emptySet()

        val totalRows = Math.ceil((firstDayOfWeek + daysInMonth) / 7.0).toInt()
        for (row in 0 until totalRows) {
            val weekRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    52.dp
                )
            }
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val day = cellIndex - firstDayOfWeek + 1
                if (day < 1 || day > daysInMonth) {
                    weekRow.addView(createEmptyCell())
                } else {
                    val isFutureDay = isFutureMonth || (isCurrentMonth && day > todayDayOfMonth)
                    weekRow.addView(createDayCell(day, todayDay, hasRecordDays, recordingDays, isFutureDay))
                }
            }
            gridCalendar.addView(weekRow)
        }
    }

    private fun createEmptyCell(): View {
        val cell = View(requireContext())
        cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        return cell
    }

    private fun createDayCell(
        day: Int,
        todayDay: Int,
        hasRecordDays: Set<Int>,
        recordingDays: Set<Int>,
        isFutureDay: Boolean
    ): LinearLayout {
        val cell = LinearLayout(requireContext())
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER
        cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

        // wrapper: 고정 크기 정사각형 배경 영역 (tvDay + dot 포함)
        val wrapper = LinearLayout(requireContext())
        wrapper.orientation = LinearLayout.VERTICAL
        wrapper.gravity = Gravity.CENTER_HORIZONTAL
        wrapper.layoutParams = LinearLayout.LayoutParams(42.dp, 42.dp)
        wrapper.setPadding(0, 3.dp, 0, 3.dp)

        val tvDay = TextView(requireContext())
        tvDay.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            28.dp
        )
        tvDay.text = day.toString()
        tvDay.gravity = Gravity.CENTER
        tvDay.textSize = 14f

        val dot = View(requireContext())
        val dotSize = 5.dp
        val dotParams = LinearLayout.LayoutParams(dotSize, dotSize)
        dotParams.topMargin = 1.dp
        dotParams.gravity = Gravity.CENTER_HORIZONTAL
        dot.layoutParams = dotParams
        dot.visibility = View.INVISIBLE

        when {
            day == selectedDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                if (day in recordingDays || day in hasRecordDays) {
                    dot.setBackgroundResource(R.drawable.bg_dot_brown)
                    dot.visibility = View.VISIBLE
                }
            }
            day in recordingDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_recording)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                dot.setBackgroundResource(R.drawable.bg_dot_brown)
                dot.visibility = View.VISIBLE
            }
            day in hasRecordDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_has_record)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                dot.setBackgroundResource(R.drawable.bg_dot_brown)
                dot.visibility = View.VISIBLE
            }
            day == todayDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_today)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.sub_200))
            }
            else -> {
                val color = if (isFutureDay) R.color.brown_300 else R.color.brown_500
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), color))
            }
        }

        wrapper.addView(tvDay)
        wrapper.addView(dot)
        cell.addView(wrapper)

        cell.setOnClickListener {
            if (!isFutureDay) {
                selectedDay = day
                renderCalendar()
                Handler(Looper.getMainLooper()).postDelayed({
                    listener?.onDateSelected(currentYear, currentMonth + 1, day)
                }, 150)
            }
        }

        return cell
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
