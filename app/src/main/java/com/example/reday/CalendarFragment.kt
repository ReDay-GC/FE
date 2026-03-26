package com.example.reday

import android.graphics.BitmapFactory
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.repository.RecordFragmentRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarFragment : Fragment() {

    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDay = -1

    private lateinit var tvCalendarTitle: TextView
    private lateinit var gridCalendar: LinearLayout
    private lateinit var repository: RecordFragmentRepository

    private var hasRecordDays: Set<Int> = emptySet()

    // 기억 상세 카드 뷰
    private lateinit var cardMemoryDetail: View
    private lateinit var ivDetailThumbnail: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailLocationOnImage: TextView
    private lateinit var tvDetailCount: TextView
    private lateinit var tvDetailPreview: TextView

    // 기록 없는 날 빈 상태 카드
    private lateinit var cardEmptyDay: View
    private lateinit var tvEmptyDayDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)

        val db = AppDatabase.getInstance(requireContext())
        repository = RecordFragmentRepository(db.recordFragmentDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCalendarTitle = view.findViewById(R.id.tv_calendar_title)
        gridCalendar = view.findViewById(R.id.grid_calendar)
        cardMemoryDetail = view.findViewById(R.id.card_memory_detail)
        ivDetailThumbnail = view.findViewById(R.id.iv_detail_thumbnail)
        tvDetailTitle = view.findViewById(R.id.tv_detail_title)
        tvDetailLocationOnImage = view.findViewById(R.id.tv_detail_location_on_image)
        tvDetailCount = view.findViewById(R.id.tv_detail_count)
        tvDetailPreview = view.findViewById(R.id.tv_detail_preview)
        cardEmptyDay = view.findViewById(R.id.card_empty_day)
        tvEmptyDayDate = view.findViewById(R.id.tv_empty_day_date)

        view.findViewById<android.widget.ImageButton>(R.id.btn_prev_month).setOnClickListener {
            if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
            selectedDay = -1
            hideAllDetailCards()
            loadAndRender()
        }

        view.findViewById<android.widget.ImageButton>(R.id.btn_next_month).setOnClickListener {
            if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
            selectedDay = -1
            hideAllDetailCards()
            loadAndRender()
        }

        loadAndRender()
    }

    private fun loadAndRender() {
        viewLifecycleOwner.lifecycleScope.launch {
            hasRecordDays = repository.getRecordDatesByMonth(currentYear, currentMonth + 1)
            renderCalendar()
        }
    }

    private fun renderCalendar() {
        tvCalendarTitle.text = "${currentMonth + 1}월 캘린더"
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
                    weekRow.addView(createDayCell(day, todayDay, hasRecordDays, isFutureDay))
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
        isFutureDay: Boolean
    ): LinearLayout {
        val cell = LinearLayout(requireContext())
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER
        cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

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
            day == selectedDay && day in hasRecordDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                dot.setBackgroundResource(R.drawable.bg_dot_brown)
                dot.visibility = View.VISIBLE
            }
            day == selectedDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
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

        if (!isFutureDay) {
            cell.setOnClickListener {
                selectedDay = day
                renderCalendar()
                if (day in hasRecordDays) {
                    loadMemoryDetail(day)
                } else {
                    showEmptyDayCard(day)
                }
            }
        }

        return cell
    }

    private fun hideAllDetailCards() {
        cardMemoryDetail.visibility = View.GONE
        cardEmptyDay.visibility = View.GONE
    }

    private fun showEmptyDayCard(day: Int) {
        cardMemoryDetail.visibility = View.GONE
        tvEmptyDayDate.text = "${currentMonth + 1}월 ${day}일"
        cardEmptyDay.visibility = View.VISIBLE
    }

    private fun loadMemoryDetail(day: Int) {
        val dateStr = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, day)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getFragmentsByDate(dateStr).collect { fragments ->
                if (fragments.isEmpty()) {
                    hideAllDetailCards()
                    return@collect
                }
                cardEmptyDay.visibility = View.GONE

                val memory = MemoryMapper.fromFragmentList(fragments).firstOrNull()
                    ?: return@collect

                // 썸네일
                if (!memory.thumbnailPath.isNullOrBlank()) {
                    val bitmap = loadBitmapWithCorrectOrientation(memory.thumbnailPath!!)
                    if (bitmap != null) {
                        ivDetailThumbnail.setImageBitmap(bitmap)
                        ImageViewCompat.setImageTintList(ivDetailThumbnail, null)
                    } else {
                        showDefaultThumbnail()
                    }
                } else {
                    showDefaultThumbnail()
                }

                // 제목
                val titleText = "%04d년 %02d월 %02d일의 기억".format(currentYear, currentMonth + 1, day)
                tvDetailTitle.text = titleText

                // 위치 (이미지 위)
                if (!memory.locationName.isNullOrBlank()) {
                    tvDetailLocationOnImage.text = memory.locationName
                    tvDetailLocationOnImage.visibility = View.VISIBLE
                } else {
                    tvDetailLocationOnImage.visibility = View.GONE
                }

                // 기록 수 배지
                tvDetailCount.text = "${memory.fragmentCount}개 기록"

                // 미리보기 텍스트
                if (!memory.previewText.isNullOrBlank()) {
                    tvDetailPreview.text = memory.previewText
                    tvDetailPreview.visibility = View.VISIBLE
                } else {
                    tvDetailPreview.visibility = View.GONE
                }

                cardMemoryDetail.visibility = View.VISIBLE
            }
        }
    }

    private fun showDefaultThumbnail() {
        ivDetailThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
        ImageViewCompat.setImageTintList(
            ivDetailThumbnail,
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.brown_300)
            )
        )
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
