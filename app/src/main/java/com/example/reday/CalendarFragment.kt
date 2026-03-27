package com.example.reday

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
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.repository.RecordFragmentRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.Calendar

class CalendarFragment : Fragment() {

    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDay = -1

    private lateinit var tvCalendarTitle: TextView
    private lateinit var gridCalendar: LinearLayout
    private lateinit var repository: RecordFragmentRepository

    // 기록 조각만 있는 날 (AI 생성 전)
    private var recordingDays: Set<Int> = emptySet()
    // AI 기억 생성 완료된 날
    private var memoryDays: Set<Int> = emptySet()

    // 기억 상세 카드 뷰
    private lateinit var cardMemoryDetail: View
    private lateinit var ivDetailThumbnail: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailLocationOnImage: TextView
    private lateinit var tvDetailCount: TextView
    private lateinit var tvDetailPreview: TextView

    // 기록중 카드 뷰
    private lateinit var cardRecordingDay: View
    private lateinit var tvRecordingDayDate: TextView
    private lateinit var tvRecordingDesc: TextView
    private lateinit var llRecordingFragments: LinearLayout
    private lateinit var btnGenerateAi: View

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

        cardRecordingDay = view.findViewById(R.id.card_recording_day)
        tvRecordingDayDate = view.findViewById(R.id.tv_recording_day_date)
        tvRecordingDesc = view.findViewById(R.id.tv_recording_desc)
        llRecordingFragments = view.findViewById(R.id.ll_recording_fragments)
        btnGenerateAi = view.findViewById(R.id.btn_generate_ai)

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
            recordingDays = repository.getRecordDatesByMonth(currentYear, currentMonth + 1)
            // memoryDays는 AI 기억 생성 완료된 날 — 추후 연동
            memoryDays = emptySet()
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
                    weekRow.addView(createDayCell(day, todayDay, isFutureDay))
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
            day == selectedDay && day in memoryDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                dot.setBackgroundResource(R.drawable.bg_dot_light)
                dot.visibility = View.VISIBLE
            }
            day == selectedDay && day in recordingDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                dot.setBackgroundResource(R.drawable.bg_dot_light)
                dot.visibility = View.VISIBLE
            }
            day == selectedDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
            }
            day in memoryDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_has_record)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                dot.setBackgroundResource(R.drawable.bg_dot_pink)
                dot.visibility = View.VISIBLE
            }
            day in recordingDays -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_recording)
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
                when {
                    day in memoryDays -> loadMemoryDetail(day)
                    day in recordingDays -> loadRecordingDayDetail(day)
                    else -> showEmptyDayCard(day)
                }
            }
        }

        return cell
    }

    private fun hideAllDetailCards() {
        cardMemoryDetail.visibility = View.GONE
        cardRecordingDay.visibility = View.GONE
        cardEmptyDay.visibility = View.GONE
    }

    private fun showEmptyDayCard(day: Int) {
        cardMemoryDetail.visibility = View.GONE
        cardRecordingDay.visibility = View.GONE
        tvEmptyDayDate.text = "${currentMonth + 1}월 ${day}일"
        cardEmptyDay.visibility = View.VISIBLE
    }

    private fun loadRecordingDayDetail(day: Int) {
        val dateStr = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, day)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getFragmentsByDate(dateStr).collect { fragments ->
                if (fragments.isEmpty()) {
                    hideAllDetailCards()
                    return@collect
                }

                cardMemoryDetail.visibility = View.GONE
                cardEmptyDay.visibility = View.GONE

                tvRecordingDayDate.text = "${currentMonth + 1}월 ${day}일"
                tvRecordingDesc.text = "${fragments.size}개의 기억 조각이 저장되어 있습니다\nAI를 생성하면 하나의 완성된 기억이 됩니다"

                llRecordingFragments.removeAllViews()
                fragments.forEach { fragment ->
                    llRecordingFragments.addView(createFragmentItemView(fragment))
                }

                cardRecordingDay.visibility = View.VISIBLE

                val dateStr = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, day)
                btnGenerateAi.setOnClickListener {
                    val intent = android.content.Intent(requireContext(), MemoryFragmentActivity::class.java)
                    intent.putExtra(MemoryFragmentActivity.EXTRA_DATE, dateStr)
                    startActivity(intent)
                }
            }
        }
    }

    private fun createFragmentItemView(fragment: RecordFragmentUiModel): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8.dp }
            setBackgroundResource(R.drawable.bg_card_section)
            setPadding(12.dp, 10.dp, 12.dp, 10.dp)
        }

        // 좌측 아이콘 / 썸네일
        val leftView: View = when (fragment.fragmentType) {
            FragmentType.PHOTO -> ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
                if (!fragment.photoUrl.isNullOrBlank()) {
                    val bm = loadBitmapWithCorrectOrientation(fragment.photoUrl!!)
                    if (bm != null) {
                        setImageBitmap(bm)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setBackgroundResource(R.drawable.bg_photo_preview_rounded)
                        clipToOutline = true
                    } else {
                        setImageResource(R.drawable.ic_photo_fragment)
                        setBackgroundResource(R.drawable.bg_record_icon_photo)
                        setPadding(12.dp, 12.dp, 12.dp, 12.dp)
                    }
                } else {
                    setImageResource(R.drawable.ic_photo_fragment)
                    setBackgroundResource(R.drawable.bg_record_icon_photo)
                    setPadding(12.dp, 12.dp, 12.dp, 12.dp)
                }
            }
            FragmentType.TEXT -> ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
                setImageResource(R.drawable.ic_memo_fragment)
                setBackgroundResource(R.drawable.bg_record_icon_square)
                setPadding(12.dp, 12.dp, 12.dp, 12.dp)
            }
            FragmentType.VOICE -> ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
                setImageResource(R.drawable.ic_mic)
                setBackgroundResource(R.drawable.bg_record_icon_circle)
                setPadding(12.dp, 12.dp, 12.dp, 12.dp)
                ImageViewCompat.setImageTintList(
                    this,
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.main_200)
                    )
                )
            }
        }
        row.addView(leftView)

        // 우측 콘텐츠
        val rightLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).also { it.marginStart = 12.dp }
        }

        // 상단: 타입 라벨 + 시간
        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val typeLabel = TextView(requireContext()).apply {
            text = when (fragment.fragmentType) {
                FragmentType.PHOTO -> "사진"
                FragmentType.TEXT -> "텍스트 메모"
                FragmentType.VOICE -> "음성 메모"
            }
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_700))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (fragment.fragmentType == FragmentType.PHOTO) {
                androidx.core.widget.TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    this, R.drawable.ic_photo_fragment, 0, 0, 0
                )
                compoundDrawablePadding = 4.dp
            }
        }
        topRow.addView(typeLabel)

        val timeText = TextView(requireContext()).apply {
            text = formatTime(fragment.createdAt)
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_400))
        }
        topRow.addView(timeText)
        rightLayout.addView(topRow)

        // 하단: 위치 / 내용
        val bottomLines: List<String> = when (fragment.fragmentType) {
            FragmentType.PHOTO -> listOfNotNull(fragment.locationName, fragment.contentText)
            FragmentType.TEXT -> listOfNotNull(fragment.contentText)
            FragmentType.VOICE -> listOfNotNull(
                if (fragment.durationSec != null) "${fragment.durationSec}초 음성" else null
            )
        }
        bottomLines.forEachIndexed { index, line ->
            val isLocation = fragment.fragmentType == FragmentType.PHOTO && index == 0
                    && !fragment.locationName.isNullOrBlank()
            val bottomTv = TextView(requireContext()).apply {
                text = line
                textSize = if (isLocation) 11f else 13f
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isLocation) R.color.brown_400 else R.color.brown_500
                    )
                )
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = if (index == 0) 2.dp else 1.dp }
            }
            rightLayout.addView(bottomTv)
        }

        row.addView(rightLayout)
        return row
    }

    private fun formatTime(createdAt: String): String {
        return try {
            val dt = LocalDateTime.parse(createdAt)
            val hour = dt.hour
            val minute = dt.minute
            if (hour < 12) {
                "오전 %d:%02d".format(if (hour == 0) 12 else hour, minute)
            } else {
                "오후 %d:%02d".format(if (hour == 12) 12 else hour - 12, minute)
            }
        } catch (e: DateTimeParseException) {
            ""
        }
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
                cardRecordingDay.visibility = View.GONE

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
                tvDetailTitle.text = "%04d년 %02d월 %02d일의 기억".format(currentYear, currentMonth + 1, day)

                // 위치
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
