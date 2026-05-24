package com.example.reday

import com.bumptech.glide.Glide
import com.example.reday.utils.loadBitmapWithCorrectOrientation
import com.example.reday.utils.toEmotionEmoji
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
import com.example.reday.data.local.entity.MemoryEntity
import com.example.reday.data.mapper.MemoryMapper
import com.example.reday.data.model.FragmentType
import com.example.reday.data.model.RecordFragmentUiModel
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.data.repository.RecordFragmentRepository
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.reday.utils.launchWithLoading
import com.example.reday.utils.launchWithRefresh
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private lateinit var memoryRepository: MemoryRepository

    private var recordingDays: Set<Int> = emptySet()
    private var memoryDays: Set<Int> = emptySet()
    private var allMemoriesThisMonth: List<com.example.reday.data.local.entity.MemoryEntity> = emptyList()
    private var currentDetailJob: kotlinx.coroutines.Job? = null

    // 기억 상세 카드 뷰
    private lateinit var cardMemoryDetail: View
    private lateinit var ivDetailThumbnail: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailEmotion: TextView
    private lateinit var tvDetailDateLocation: TextView
    private lateinit var tvDetailSummary: TextView
    private lateinit var llDetailMeta: LinearLayout
    private lateinit var llDetailFragments: LinearLayout
    private lateinit var chipGroupDetailTags: ChipGroup

    // 기록중 카드 뷰
    private lateinit var cardRecordingDay: View
    private lateinit var tvRecordingDayDate: TextView
    private lateinit var tvRecordingDesc: TextView
    private lateinit var llRecordingFragments: LinearLayout
    private lateinit var btnGenerateAi: View

    // 기록 없는 날 빈 상태 카드
    private lateinit var cardEmptyDay: View
    private lateinit var tvEmptyDayDate: TextView

    private lateinit var pbLoading: View
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)

        repository = RecordFragmentRepository()
        memoryRepository = MemoryRepository()
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
        tvDetailEmotion = view.findViewById(R.id.tv_detail_emotion)
        tvDetailDateLocation = view.findViewById(R.id.tv_detail_date_location)
        tvDetailSummary = view.findViewById(R.id.tv_detail_summary)
        llDetailMeta = view.findViewById(R.id.ll_detail_meta)
        llDetailFragments = view.findViewById(R.id.ll_detail_fragments)
        chipGroupDetailTags = view.findViewById(R.id.chip_group_detail_tags)

        cardRecordingDay = view.findViewById(R.id.card_recording_day)
        tvRecordingDayDate = view.findViewById(R.id.tv_recording_day_date)
        tvRecordingDesc = view.findViewById(R.id.tv_recording_desc)
        llRecordingFragments = view.findViewById(R.id.ll_recording_fragments)
        btnGenerateAi = view.findViewById(R.id.btn_generate_ai)

        cardEmptyDay = view.findViewById(R.id.card_empty_day)
        tvEmptyDayDate = view.findViewById(R.id.tv_empty_day_date)
        pbLoading = view.findViewById(R.id.pb_loading)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.main_200)
        swipeRefresh.setOnRefreshListener { launchWithRefresh(swipeRefresh) { loadCalendarData() } }

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

    }

    override fun onResume() {
        super.onResume()
        loadAndRender()
    }

    private fun loadAndRender() {
        launchWithLoading(pbLoading) { loadCalendarData() }
    }

    private suspend fun loadCalendarData() {
        try {
            val fragmentDays = repository.getRecordDatesByMonth(currentYear, currentMonth + 1)
            allMemoriesThisMonth = memoryRepository.getMemoriesByMonth(currentYear, currentMonth + 1)
            memoryDays = allMemoriesThisMonth.mapNotNull {
                it.date.split("-").getOrNull(2)?.toIntOrNull()
            }.toSet()
            recordingDays = fragmentDays - memoryDays
            android.util.Log.d("CalendarDebug", "fragmentDays=$fragmentDays")
            android.util.Log.d("CalendarDebug", "memoryDays=$memoryDays")
            android.util.Log.d("CalendarDebug", "recordingDays=$recordingDays")
        } catch (e: Exception) {
            android.util.Log.e("CalendarDebug", "loadCalendarData 실패: ${e.message}")
        }
        renderCalendar()
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

    private fun createDayCell(day: Int, todayDay: Int, isFutureDay: Boolean): LinearLayout {
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
        tvDay.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 28.dp)
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
            day == selectedDay && day in memoryDays && !isFutureDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                dot.setBackgroundResource(R.drawable.bg_dot_light)
                dot.visibility = View.VISIBLE
            }
            day == selectedDay && day in recordingDays && !isFutureDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
                dot.setBackgroundResource(R.drawable.bg_dot_light)
                dot.visibility = View.VISIBLE
            }
            day == selectedDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_selected)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_50))
            }
            day in memoryDays && !isFutureDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_has_record)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                dot.setBackgroundResource(R.drawable.bg_dot_pink)
                dot.visibility = View.VISIBLE
            }
            day in recordingDays && !isFutureDay -> {
                wrapper.setBackgroundResource(R.drawable.bg_calendar_recording)
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                dot.setBackgroundResource(R.drawable.bg_dot_main200)
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
                val dateStr = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, day)
                currentDetailJob?.cancel()
                currentDetailJob = viewLifecycleOwner.lifecycleScope.launch {
                    val hasFragments = repository.getFragmentsByDate(dateStr).isNotEmpty()
                    if (hasFragments) loadAndRender()
                    when {
                        day in memoryDays -> loadMemoryDetail(day)
                        hasFragments -> loadRecordingDayDetail(day)
                        else -> showEmptyDayCard(day)
                    }
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
        currentDetailJob?.cancel()
        currentDetailJob = viewLifecycleOwner.lifecycleScope.launch {
            val fragments = repository.getFragmentsByDate(dateStr)
            if (fragments.isEmpty()) {
                hideAllDetailCards()
                return@launch
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

            btnGenerateAi.setOnClickListener {
                val intent = android.content.Intent(requireContext(), MemoryFragmentActivity::class.java)
                intent.putExtra(MemoryFragmentActivity.EXTRA_DATE, dateStr)
                startActivity(intent)
            }
        }
    }

    private fun loadMemoryDetail(day: Int) {
        val dateStr = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, day)
        currentDetailJob?.cancel()
        currentDetailJob = viewLifecycleOwner.lifecycleScope.launch {
            val entity = allMemoriesThisMonth.firstOrNull { it.date == dateStr }
                ?: memoryRepository.getMemoryByDate(dateStr)
                ?: run { hideAllDetailCards(); return@launch }

            cardEmptyDay.visibility = View.GONE
            cardRecordingDay.visibility = View.GONE

            cardMemoryDetail.setOnClickListener {
                val intent = android.content.Intent(requireContext(), MemoryDetailActivity::class.java)
                intent.putExtra(MemoryDetailActivity.EXTRA_DATE, dateStr)
                intent.putExtra(MemoryDetailActivity.EXTRA_SERVER_ID, entity.serverId ?: 0L)
                startActivity(intent)
            }

            // 썸네일
            if (!entity.representativePhotoUrl.isNullOrBlank()) {
                val url = entity.representativePhotoUrl!!
                ImageViewCompat.setImageTintList(ivDetailThumbnail, null)
                viewLifecycleOwner.lifecycleScope.launch {
                    val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        loadBitmapWithCorrectOrientation(url)
                    }
                    if (bitmap != null) {
                        ivDetailThumbnail.setImageBitmap(bitmap)
                        ivDetailThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
            } else {
                showDefaultThumbnail()
            }

            // 날짜 + 첫 번째 위치
            val memory = MemoryMapper.fromMemoryEntity(entity)

            // 제목
            tvDetailTitle.text = memory.title

            // 감정 이모지
            val emotionEmoji = memory.emotion.toEmotionEmoji()
            if (emotionEmoji != null) {
                tvDetailEmotion.text = emotionEmoji
                tvDetailEmotion.visibility = View.VISIBLE
            } else {
                tvDetailEmotion.visibility = View.GONE
            }
            tvDetailDateLocation.text = buildString {
                append(entity.date)
                if (!memory.locationName.isNullOrBlank()) {
                    append(" • ")
                    append(memory.locationName)
                }
            }

            // AI 요약
            tvDetailSummary.text = entity.summary

            // 위치 + 사람 메타 행
            buildMetaRow(entity)

            // 기록 조각 목록
            val fragments = repository.getFragmentsByDate(dateStr)
            llDetailFragments.removeAllViews()
            fragments.forEach { fragment ->
                llDetailFragments.addView(createDetailFragmentItem(fragment))
            }

            // 태그
            val tagType = object : TypeToken<List<String>>() {}.type
            val tags = try { Gson().fromJson<List<String>>(entity.tags, tagType) } catch (e: Exception) { emptyList() }
            chipGroupDetailTags.removeAllViews()
            tags.forEach { tag ->
                chipGroupDetailTags.addView(createTagChip(tag))
            }

            cardMemoryDetail.visibility = View.VISIBLE
        }
    }

    private fun buildMetaRow(entity: MemoryEntity) {
        llDetailMeta.removeAllViews()
        val listType = object : TypeToken<List<String>>() {}.type
        val locations = try { Gson().fromJson<List<String>>(entity.locations, listType) } catch (e: Exception) { emptyList() }
        val people = try { Gson().fromJson<List<String>>(entity.people, listType) } catch (e: Exception) { emptyList() }

        var hasItem = false

        if (locations.isNotEmpty()) {
            llDetailMeta.addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).also { it.marginEnd = 4.dp }
                setImageResource(R.drawable.ic_location)
                ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brown_400)))
            })
            llDetailMeta.addView(TextView(requireContext()).apply {
                text = locations.first()
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 10.dp }
            })
            hasItem = true
        }

        if (people.isNotEmpty()) {
            if (hasItem) {
                llDetailMeta.addView(TextView(requireContext()).apply {
                    text = "•"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_300))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.marginEnd = 10.dp }
                })
            }
            llDetailMeta.addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).also { it.marginEnd = 4.dp }
                setImageResource(R.drawable.ic_person)
                ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brown_400)))
            })
            llDetailMeta.addView(TextView(requireContext()).apply {
                text = people.joinToString(", ")
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_500))
            })
        }

        llDetailMeta.visibility = if (locations.isNotEmpty() || people.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun createDetailFragmentItem(fragment: RecordFragmentUiModel): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8.dp }
            setBackgroundResource(R.drawable.bg_card_section)
            setPadding(14.dp, 12.dp, 14.dp, 12.dp)
        }

        when (fragment.fragmentType) {
            FragmentType.PHOTO -> {
                // 텍스트(왼쪽) + 작은 썸네일(오른쪽)
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val textCol = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .also { it.marginEnd = 10.dp }
                }
                val displayText = fragment.contentText?.takeIf { it.isNotBlank() } ?: "사진 기록"
                textCol.addView(createRecordTextView(displayText))
                textCol.addView(TextView(requireContext()).apply {
                    text = formatTime(fragment.createdAt)
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_400))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = 4.dp }
                })
                row.addView(textCol)
                if (!fragment.photoUrl.isNullOrBlank()) {
                    val thumbIv = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(64.dp, 64.dp)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setBackgroundResource(R.drawable.bg_photo_preview_rounded)
                        clipToOutline = true
                    }
                    val url = fragment.photoUrl!!
                    viewLifecycleOwner.lifecycleScope.launch {
                        val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            loadBitmapWithCorrectOrientation(url)
                        }
                        if (bitmap != null) {
                            thumbIv.setImageBitmap(bitmap)
                            thumbIv.scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    }
                    row.addView(thumbIv)
                }
                card.addView(row)
                return card
            }
            FragmentType.TEXT -> {
                if (!fragment.contentText.isNullOrBlank()) {
                    card.addView(createRecordTextView(fragment.contentText!!))
                }
            }
            FragmentType.VOICE -> {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 4.dp }
                }
                row.addView(ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(18.dp, 18.dp).also { it.marginEnd = 6.dp }
                    setImageResource(R.drawable.ic_mic)
                    ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.main_200)))
                })
                row.addView(TextView(requireContext()).apply {
                    text = "${fragment.durationSec ?: 0}초 음성 메모"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_600))
                })
                card.addView(row)
            }
        }

        // 시간 (TEXT, VOICE 공통)
        card.addView(TextView(requireContext()).apply {
            text = formatTime(fragment.createdAt)
            textSize = 11f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_400))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 4.dp }
        })

        return card
    }

    private fun createRecordTextView(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_700))
            setLineSpacing(0f, 1.4f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createTagChip(tag: String): Chip {
        return Chip(requireContext()).apply {
            text = "#$tag"
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.sub_100))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.sub_200))
            chipStrokeWidth = 0f
            isClickable = false
            isCheckable = false
            textSize = 12f
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(999f)
                .build()
        }
    }

    private fun createFragmentItemView(fragment: RecordFragmentUiModel): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8.dp }
            setBackgroundResource(R.drawable.bg_card_section)
            setPadding(12.dp, 10.dp, 12.dp, 10.dp)
        }

        val leftView: View = when (fragment.fragmentType) {
            FragmentType.PHOTO -> ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
                if (!fragment.photoUrl.isNullOrBlank()) {
                    val url = fragment.photoUrl!!
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true
                    val iv = this
                    viewLifecycleOwner.lifecycleScope.launch {
                        val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            loadBitmapWithCorrectOrientation(url)
                        }
                        if (bitmap != null) iv.setImageBitmap(bitmap)
                        else iv.setImageResource(R.drawable.ic_photo_fragment)
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
                ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.main_200)))
            }
        }
        row.addView(leftView)

        val rightLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = 12.dp }
        }

        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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
        }
        topRow.addView(typeLabel)

        val timeText = TextView(requireContext()).apply {
            text = formatTime(fragment.createdAt)
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_400))
        }
        topRow.addView(timeText)
        rightLayout.addView(topRow)

        val bottomLines: List<String> = when (fragment.fragmentType) {
            FragmentType.PHOTO -> listOfNotNull(fragment.locationName, fragment.contentText)
            FragmentType.TEXT -> listOfNotNull(fragment.contentText)
            FragmentType.VOICE -> listOfNotNull(
                if (fragment.durationSec != null) "${fragment.durationSec}초 음성" else null)
        }
        bottomLines.forEachIndexed { index, line ->
            val isLocation = fragment.fragmentType == FragmentType.PHOTO && index == 0
                    && !fragment.locationName.isNullOrBlank()
            rightLayout.addView(TextView(requireContext()).apply {
                text = line
                textSize = if (isLocation) 11f else 13f
                setTextColor(ContextCompat.getColor(requireContext(),
                    if (isLocation) R.color.brown_400 else R.color.brown_500))
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = if (index == 0) 2.dp else 1.dp }
            })
        }

        row.addView(rightLayout)
        return row
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
        } catch (e: DateTimeParseException) { "" }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}
