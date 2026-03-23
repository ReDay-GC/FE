package com.example.reday

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
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

    // 더미데이터 (추후 repository에서 공급 예정)
    private val hasRecordDays = setOf(3, 5, 8)
    private val recordingDays = setOf(10)

    private lateinit var tvMonthYear: TextView
    private lateinit var gridCalendar: GridLayout

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
            if (currentMonth == 0) {
                currentMonth = 11
                currentYear--
            } else {
                currentMonth--
            }
            selectedDay = -1
            renderCalendar()
        }

        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener {
            if (currentMonth == 11) {
                currentMonth = 0
                currentYear++
            } else {
                currentMonth++
            }
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
        val isCurrentMonth = todayCal.get(Calendar.YEAR) == currentYear
                && todayCal.get(Calendar.MONTH) == currentMonth
        val todayDay = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

        repeat(firstDayOfWeek) {
            gridCalendar.addView(createEmptyCell())
        }

        for (day in 1..daysInMonth) {
            gridCalendar.addView(createDayCell(day, todayDay))
        }
    }

    private fun createEmptyCell(): View {
        val cell = View(requireContext())
        cell.layoutParams = createCellParams()
        return cell
    }

    private fun createDayCell(day: Int, todayDay: Int): LinearLayout {
        val cell = LinearLayout(requireContext())
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER_HORIZONTAL
        cell.layoutParams = createCellParams()
        cell.setPadding(0, 4.dp, 0, 4.dp)

        val tvDay = TextView(requireContext())
        val circleSize = 36.dp
        val tvParams = LinearLayout.LayoutParams(circleSize, circleSize)
        tvDay.layoutParams = tvParams
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
                if (day in recordingDays) {
                    dot.setBackgroundResource(R.drawable.bg_dot_light)
                    dot.visibility = View.VISIBLE
                } else if (day in hasRecordDays) {
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
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_800))
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

    private fun createCellParams(): GridLayout.LayoutParams {
        val params = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        )
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        return params
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
