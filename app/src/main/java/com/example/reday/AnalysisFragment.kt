package com.example.reday

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.ActivityStatItem
import com.example.reday.data.remote.GenerateInsightRequest
import com.example.reday.data.remote.MemorySummary
import com.example.reday.data.remote.MonthlyAnalysisData
import com.example.reday.data.remote.PeopleStatItem
import com.example.reday.data.remote.PlaceStatItem
import com.example.reday.data.remote.RecordTypeStatItem
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.utils.TokenManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalysisFragment : Fragment() {

    private lateinit var memoryRepository: MemoryRepository

    private lateinit var layoutEmpty: View
    private lateinit var layoutContent: View
    private lateinit var layoutInsightEmpty: View
    private lateinit var layoutInsightContent: View
    private lateinit var layoutInsightLoading: View
    private lateinit var tvInsightTitle: TextView
    private lateinit var tvInsightText: TextView
    private lateinit var chartMonthly: BarChart
    private lateinit var chartActivity: PieChart
    private lateinit var chartRecordType: PieChart
    private lateinit var layoutPlaces: LinearLayout
    private lateinit var layoutPeople: LinearLayout

    private val currentYearMonth: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memoryRepository = MemoryRepository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutEmpty = view.findViewById(R.id.layout_empty)
        layoutContent = view.findViewById(R.id.layout_content)
        layoutInsightEmpty = view.findViewById(R.id.layout_insight_empty)
        layoutInsightContent = view.findViewById(R.id.layout_insight_content)
        layoutInsightLoading = view.findViewById(R.id.layout_insight_loading)
        tvInsightTitle = view.findViewById(R.id.tv_insight_title)
        tvInsightText = view.findViewById(R.id.tv_insight_text)
        chartMonthly = view.findViewById(R.id.chart_monthly)
        chartActivity = view.findViewById(R.id.chart_activity)
        chartRecordType = view.findViewById(R.id.chart_record_type)
        layoutPlaces = view.findViewById(R.id.layout_places)
        layoutPeople = view.findViewById(R.id.layout_people)

        view.findViewById<View>(R.id.btn_generate_insight).setOnClickListener {
            generateInsight()
        }
        view.findViewById<View>(R.id.btn_regenerate_insight).setOnClickListener {
            generateInsight()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val today = LocalDate.now()
            val analysisData = memoryRepository.getMonthlyAnalysis(today.year, today.monthValue)

            val totalMemories = analysisData?.memoryTrend?.sumOf { it.count } ?: 0
            if (analysisData == null || totalMemories == 0) {
                layoutEmpty.visibility = View.VISIBLE
                layoutContent.visibility = View.GONE
                return@launch
            }

            layoutEmpty.visibility = View.GONE
            layoutContent.visibility = View.VISIBLE

            setupMonthlyChart(analysisData.memoryTrend)
            setupActivityChart(analysisData.topActivities)
            setupRecordTypeChart(analysisData.recordTypeStats)
            setupPlaces(analysisData.topPlaces)
            setupPeople(analysisData.topPeople)

            if (!analysisData.monthlyInsight.isNullOrBlank()) {
                showInsightContent(analysisData.monthlyInsight)
            } else {
                layoutInsightEmpty.visibility = View.VISIBLE
                layoutInsightContent.visibility = View.GONE
                layoutInsightLoading.visibility = View.GONE
            }
        }
    }

    private fun generateInsight() {
        viewLifecycleOwner.lifecycleScope.launch {
            layoutInsightEmpty.visibility = View.GONE
            layoutInsightContent.visibility = View.GONE
            layoutInsightLoading.visibility = View.VISIBLE

            try {
                val yearMonth = currentYearMonth
                val userId = TokenManager.getUserId(requireContext())
                val memories = memoryRepository.getAllMemories()
                    .filter { it.date.startsWith(yearMonth) }

                if (memories.isEmpty()) {
                    Toast.makeText(requireContext(), "이번 달 기억이 없어요", Toast.LENGTH_SHORT).show()
                    layoutInsightEmpty.visibility = View.VISIBLE
                    layoutInsightLoading.visibility = View.GONE
                    return@launch
                }

                val summaries = memories.map { it.toMemorySummary() }
                val request = GenerateInsightRequest(
                    year_month = yearMonth,
                    memories = summaries,
                    user_id = userId
                )
                val response = RetrofitClient.memoryApi.generateInsight(request)
                showInsightContent(response.insight)

                // 인사이트 생성 후 서버 데이터 갱신 (topActivities, topPeople 포함)
                val today = LocalDate.now()
                val updated = memoryRepository.getMonthlyAnalysis(today.year, today.monthValue)
                updated?.topActivities?.let { setupActivityChart(it) }
                updated?.topPeople?.let { setupPeople(it) }

            } catch (e: Exception) {
                layoutInsightLoading.visibility = View.GONE
                layoutInsightEmpty.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "인사이트 생성에 실패했어요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInsightContent(text: String) {
        val month = currentYearMonth.split("-").getOrNull(1)?.toIntOrNull() ?: 0
        tvInsightTitle.text = "✦ ${month}월의 인사이트"
        tvInsightText.text = text
        layoutInsightLoading.visibility = View.GONE
        layoutInsightEmpty.visibility = View.GONE
        layoutInsightContent.visibility = View.VISIBLE
    }

    // ── 차트 ──

    private fun setupMonthlyChart(trend: List<com.example.reday.data.remote.MemoryTrendItem>) {
        if (trend.isEmpty()) {
            chartMonthly.visibility = View.GONE
            return
        }

        val labels = trend.map { "${it.month}월" }
        val entries = trend.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.count.toFloat())
        }

        val barColor = ContextCompat.getColor(requireContext(), R.color.main_200)
        val dataSet = BarDataSet(entries, "").apply {
            color = barColor
            setDrawValues(false)
        }

        chartMonthly.apply {
            data = BarData(dataSet).apply { barWidth = 0.5f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.brown_500)
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.brown_200)
                enableGridDashedLine(8f, 8f, 0f)
                setDrawAxisLine(false)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(requireContext(), R.color.brown_500)
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun setupActivityChart(activities: List<ActivityStatItem>?) {
        if (activities.isNullOrEmpty()) {
            chartActivity.visibility = View.GONE
            return
        }

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.sub_200),
            ContextCompat.getColor(requireContext(), R.color.main_200),
            ContextCompat.getColor(requireContext(), R.color.sub_105),
            ContextCompat.getColor(requireContext(), R.color.main_105),
            ContextCompat.getColor(requireContext(), R.color.brown_400),
            ContextCompat.getColor(requireContext(), R.color.brown_300)
        )

        val entries = activities.map { item ->
            PieEntry(item.percentage.toFloat(), "${item.activityType} ${item.percentage}%")
        }

        setupPieChart(chartActivity, entries, colors)
    }

    private fun setupRecordTypeChart(stats: List<RecordTypeStatItem>) {
        val total = stats.sumOf { it.count }
        if (total == 0) {
            chartRecordType.visibility = View.GONE
            return
        }

        val colorMap = mapOf(
            "PHOTO" to ContextCompat.getColor(requireContext(), R.color.main_200),
            "TEXT" to ContextCompat.getColor(requireContext(), R.color.brown_400),
            "VOICE" to ContextCompat.getColor(requireContext(), R.color.sub_200)
        )
        val labelMap = mapOf("PHOTO" to "사진", "TEXT" to "텍스트", "VOICE" to "음성")

        val filtered = stats.filter { it.count > 0 }
        val entries = filtered.map { stat ->
            PieEntry(stat.count.toFloat(), "${labelMap[stat.recordType] ?: stat.recordType} ${stat.percentage.toInt()}%")
        }
        val colors = filtered.map { colorMap[it.recordType] ?: Color.GRAY }

        setupPieChart(chartRecordType, entries, colors)
    }

    private fun setupPlaces(places: List<PlaceStatItem>) {
        val items = places.map { it.place to it.count }
        val barColor = ContextCompat.getColor(requireContext(), R.color.sub_200)
        val countColor = ContextCompat.getColor(requireContext(), R.color.sub_105)
        fillRankLayout(layoutPlaces, items, barColor, countColor)
    }

    private fun setupPeople(people: List<PeopleStatItem>?) {
        val items = people?.map { it.name to it.count } ?: emptyList()
        val barColor = ContextCompat.getColor(requireContext(), R.color.main_200)
        val countColor = ContextCompat.getColor(requireContext(), R.color.main_200)
        fillRankLayout(layoutPeople, items, barColor, countColor)
    }

    private fun setupPieChart(chart: PieChart, entries: List<PieEntry>, colors: List<Int>) {
        val lineColor = ContextCompat.getColor(requireContext(), R.color.brown_300)
        val textColor = ContextCompat.getColor(requireContext(), R.color.brown_700)

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            setDrawValues(false)
            setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
            setValueLineColor(lineColor)
            setValueLineWidth(1f)
            setValueLinePart1Length(0.4f)
            setValueLinePart2Length(0.6f)
            setValueLinePart1OffsetPercentage(85f)
            setValueTextColor(textColor)
            valueTextSize = 11f
        }

        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 35f
            transparentCircleRadius = 40f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setDrawEntryLabels(true)
            setEntryLabelColor(textColor)
            setEntryLabelTextSize(11f)
            legend.isEnabled = false
            setTouchEnabled(false)
            extraLeftOffset = 30f
            extraRightOffset = 30f
            extraTopOffset = 10f
            extraBottomOffset = 10f
            invalidate()
        }
    }

    private fun fillRankLayout(
        container: LinearLayout,
        items: List<Pair<String, Int>>,
        barColor: Int,
        countColor: Int = ContextCompat.getColor(requireContext(), R.color.brown_500)
    ) {
        container.removeAllViews()
        if (items.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "데이터가 없어요"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_400))
            }
            container.addView(tv)
            return
        }

        val maxCount = items.first().second

        items.forEachIndexed { index, (name, count) ->
            val itemView = layoutInflater.inflate(R.layout.item_rank, container, false)
            itemView.findViewById<TextView>(R.id.tv_rank).text = "${index + 1}"
            itemView.findViewById<TextView>(R.id.tv_rank_name).text = name
            itemView.findViewById<TextView>(R.id.tv_rank_count).apply {
                text = "${count}회"
                setTextColor(countColor)
            }

            val progressBar = itemView.findViewById<ProgressBar>(R.id.progress_rank)
            progressBar.max = maxCount
            progressBar.progress = count
            progressBar.progressTintList =
                android.content.res.ColorStateList.valueOf(barColor)

            container.addView(itemView)
        }
    }

    private fun com.example.reday.data.local.entity.MemoryEntity.toMemorySummary(): MemorySummary {
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
        val tags = try { gson.fromJson<List<String>>(this.tags, listType) ?: emptyList() } catch (e: Exception) { emptyList() }
        val locations = try { gson.fromJson<List<String>>(this.locations, listType) ?: emptyList() } catch (e: Exception) { emptyList() }
        val people = try { gson.fromJson<List<String>>(this.people, listType) ?: emptyList() } catch (e: Exception) { emptyList() }
        return MemorySummary(
            date = this.date,
            title = this.title,
            summary = this.summary,
            tags = tags,
            locations = locations,
            people = people
        )
    }
}
