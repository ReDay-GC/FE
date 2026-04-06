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
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.local.entity.MemoryEntity
import com.example.reday.data.model.FragmentType
import com.example.reday.data.remote.GenerateInsightRequest
import com.example.reday.data.remote.MemorySummary
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.data.repository.MonthlyInsightRepository
import com.example.reday.data.repository.RecordFragmentRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalysisFragment : Fragment() {

    private lateinit var memoryRepository: MemoryRepository
    private lateinit var insightRepository: MonthlyInsightRepository
    private lateinit var fragmentRepository: RecordFragmentRepository

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
        val db = AppDatabase.getInstance(requireContext())
        memoryRepository = MemoryRepository(db.memoryDao())
        insightRepository = MonthlyInsightRepository(db.monthlyInsightDao())
        fragmentRepository = RecordFragmentRepository(db.recordFragmentDao())
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

        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allMemories = memoryRepository.getAllMemories().first()

            if (allMemories.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                layoutContent.visibility = View.GONE
                return@launch
            }

            layoutEmpty.visibility = View.GONE
            layoutContent.visibility = View.VISIBLE

            val allFragments = fragmentRepository.getAllFragments().first()

            setupMonthlyChart(allMemories)
            setupActivityChart(allMemories)
            setupRecordTypeChart(
                photo = allFragments.count { it.fragmentType == FragmentType.PHOTO },
                text = allFragments.count { it.fragmentType == FragmentType.TEXT },
                voice = allFragments.count { it.fragmentType == FragmentType.VOICE }
            )
            setupPlaces(allFragments)
            setupPeople(allMemories)
            loadInsight()
        }
    }

    private suspend fun loadInsight() {
        val saved = insightRepository.getInsight(currentYearMonth)
        if (saved != null) {
            showInsightContent(saved.insightText)
        } else {
            layoutInsightEmpty.visibility = View.VISIBLE
            layoutInsightContent.visibility = View.GONE
            layoutInsightLoading.visibility = View.GONE
        }
    }

    private fun generateInsight() {
        viewLifecycleOwner.lifecycleScope.launch {
            layoutInsightEmpty.visibility = View.GONE
            layoutInsightContent.visibility = View.GONE
            layoutInsightLoading.visibility = View.VISIBLE

            try {
                val yearMonth = currentYearMonth
                val memories = memoryRepository.getAllMemories().first()
                    .filter { it.date.startsWith(yearMonth) }

                if (memories.isEmpty()) {
                    Toast.makeText(requireContext(), "이번 달 기억이 없어요", Toast.LENGTH_SHORT).show()
                    layoutInsightEmpty.visibility = View.VISIBLE
                    layoutInsightLoading.visibility = View.GONE
                    return@launch
                }

                val summaries = memories.map { it.toMemorySummary() }
                val request = GenerateInsightRequest(year_month = yearMonth, memories = summaries)
                val response = RetrofitClient.memoryApi.generateInsight(request)

                insightRepository.saveInsight(yearMonth, response.insight)
                showInsightContent(response.insight)

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

    private fun setupMonthlyChart(memories: List<MemoryEntity>) {
        val today = LocalDate.now()
        val months = (5 downTo 0).map { today.minusMonths(it.toLong()) }
        val labels = months.map { "${it.monthValue}월" }

        val entries = months.mapIndexed { index, date ->
            val ym = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val count = memories.count { it.date.startsWith(ym) }
            BarEntry(index.toFloat(), count.toFloat())
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

    private fun setupActivityChart(memories: List<MemoryEntity>) {
        val tagCounts = mutableMapOf<String, Int>()
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type

        memories.forEach { memory ->
            try {
                val tags = gson.fromJson<List<String>>(memory.tags, type) ?: emptyList()
                tags.forEach { tag -> tagCounts[tag] = (tagCounts[tag] ?: 0) + 1 }
            } catch (e: Exception) { /* skip */ }
        }

        val top = tagCounts.entries.sortedByDescending { it.value }.take(6)
        if (top.isEmpty()) {
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

        val total = top.sumOf { it.value }.toFloat()
        val entries = top.map { (tag, count) ->
            PieEntry(count.toFloat(), "$tag ${(count / total * 100).toInt()}%")
        }

        setupPieChart(chartActivity, entries, colors)
    }

    private fun setupRecordTypeChart(photo: Int, text: Int, voice: Int) {
        val total = photo + text + voice
        if (total == 0) {
            chartRecordType.visibility = View.GONE
            return
        }

        val entries = mutableListOf<PieEntry>()
        if (photo > 0) entries.add(PieEntry(photo.toFloat(), "사진 ${(photo * 100 / total)}%"))
        if (text > 0) entries.add(PieEntry(text.toFloat(), "텍스트 ${(text * 100 / total)}%"))
        if (voice > 0) entries.add(PieEntry(voice.toFloat(), "음성 ${(voice * 100 / total)}%"))

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.main_200),
            ContextCompat.getColor(requireContext(), R.color.brown_400),
            ContextCompat.getColor(requireContext(), R.color.sub_200)
        )

        setupPieChart(chartRecordType, entries, colors)
    }

    private fun setupPieChart(chart: PieChart, entries: List<PieEntry>, colors: List<Int>) {
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            setDrawValues(false)
        }

        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = false
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.brown_700))
            setEntryLabelTextSize(11f)
            legend.isEnabled = false
            setTouchEnabled(false)
            invalidate()
        }
    }

    private fun setupPlaces(fragments: List<com.example.reday.data.model.RecordFragmentUiModel>) {
        val located = fragments.filter {
            it.latitude != null && it.longitude != null && !it.locationName.isNullOrBlank()
        }

        if (located.isEmpty()) {
            fillRankLayout(layoutPlaces, emptyList(), ContextCompat.getColor(requireContext(), R.color.sub_200))
            return
        }

        // 좌표 기반 클러스터링 (300m 이내 = 같은 장소)
        data class Cluster(
            val centerLat: Double,
            val centerLng: Double,
            val names: MutableList<String> = mutableListOf()
        )

        val clusters = mutableListOf<Cluster>()

        located.forEach { fragment ->
            val lat = fragment.latitude!!
            val lng = fragment.longitude!!
            val name = fragment.locationName!!

            val nearby = clusters.find { haversineDistance(it.centerLat, it.centerLng, lat, lng) <= 300.0 }
            if (nearby != null) {
                nearby.names.add(name)
            } else {
                clusters.add(Cluster(lat, lng, mutableListOf(name)))
            }
        }

        // 각 클러스터의 대표 이름: 가장 많이 등장한 이름, 동률이면 가장 짧은 이름
        val top = clusters.map { cluster ->
            val representativeName = cluster.names
                .groupingBy { it }
                .eachCount()
                .entries
                .maxWithOrNull(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
                ?.key ?: cluster.names.first()
            representativeName to cluster.names.size
        }.sortedByDescending { it.second }.take(4)

        val barColor = ContextCompat.getColor(requireContext(), R.color.sub_200)
        fillRankLayout(layoutPlaces, top, barColor)
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun setupPeople(memories: List<MemoryEntity>) {
        val peopleCounts = mutableMapOf<String, Int>()
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type

        memories.forEach { memory ->
            try {
                val people = gson.fromJson<List<String>>(memory.people, type) ?: emptyList()
                people.forEach { person -> peopleCounts[person] = (peopleCounts[person] ?: 0) + 1 }
            } catch (e: Exception) { /* skip */ }
        }

        val top = peopleCounts.entries.sortedByDescending { it.value }.take(4).map { it.key to it.value }
        val barColor = ContextCompat.getColor(requireContext(), R.color.main_200)
        fillRankLayout(layoutPeople, top, barColor)
    }

    private fun fillRankLayout(
        container: LinearLayout,
        items: List<Pair<String, Int>>,
        barColor: Int
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
            itemView.findViewById<TextView>(R.id.tv_rank_count).text = "${count}회"

            val progressBar = itemView.findViewById<ProgressBar>(R.id.progress_rank)
            progressBar.max = maxCount
            progressBar.progress = count
            progressBar.progressTintList =
                android.content.res.ColorStateList.valueOf(barColor)

            container.addView(itemView)
        }
    }

    private fun MemoryEntity.toMemorySummary(): MemorySummary {
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
