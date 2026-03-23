package com.example.reday

import android.os.Bundle
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

        view.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        view.findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener {
            if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
            selectedDay = -1
            renderCalendar()
        }

        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener {
            if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
            selectedDay = -1
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

        // 항상 6행으로 고정 → 높이 안정적, 범례 항상 표시됨
        for (row in 0 until 6) {
            val weekRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
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
        cell.gravity = Gravity.CENTER_HORIZONTAL
        cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        cell.setPadding(0, 4.dp, 0, 4.dp)

        val tvDay = TextView(requireContext())
        val circleSize = 36.dp
        tvDay.layoutParams = LinearLayout.LayoutParams(circleSize, circleSize)
        tvDay.text = day.toString()
        tvDay.gravity = Gravity.CENTER
        tvDay.textSize = 14f

        val dot = View(requireContext())
        val dotSize = 5.dp
        val dotParams = LinearLayout.LayoutParams(dotSize, dotSize)
        dotParams.topMargin = 2.dp
        dot.layoutParams = dotParams
        dot.visibility = View.INVISIBLE

        when {
            day == selectedDay -> {
                tvDay.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                if (day in recordingDays || day in hasRecordDays) {
                    dot.setBackgroundResource(R.drawable.bg_dot_light)
                    dot.visibility = View.VISIBLE
                }
            }
            day in recordingDays -> {
                tvDay.setBackgroundResource(R.drawable.bg_calendar_recording)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                dot.setBackgroundResource(R.drawable.bg_dot_light)
                dot.visibility = View.VISIBLE
            }
            day in hasRecordDays -> {
                tvDay.setBackgroundResource(R.drawable.bg_calendar_has_record)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_800))
                dot.setBackgroundResource(R.drawable.bg_dot_pink)
                dot.visibility = View.VISIBLE
            }
            day == todayDay -> {
                tvDay.setBackgroundResource(R.drawable.bg_calendar_today)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_800))
            }
            else -> {
                val color = if (isFutureDay) R.color.brown_300 else R.color.brown_800
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), color))
            }
        }

        cell.addView(tvDay)
        cell.addView(dot)

        cell.setOnClickListener {
            selectedDay = day
            renderCalendar()
        }

        return cell
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
