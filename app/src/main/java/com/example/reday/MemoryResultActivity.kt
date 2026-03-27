package com.example.reday

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.time.DayOfWeek
import java.time.LocalDate

class MemoryResultActivity : AppCompatActivity() {

    private val ALL_TAGS = listOf("여행", "카페", "산책", "공부", "운동", "유흥", "자연", "문화생활", "쇼핑", "식사", "휴식")

    private var isEditMode = false
    private val selectedTags = mutableSetOf<String>()
    private val locationList = mutableListOf<String>()
    private val peopleList = mutableListOf<String>()

    private lateinit var chipGroupTagsView: ChipGroup
    private lateinit var chipGroupTagsEdit: ChipGroup
    private lateinit var tvLocationLabel: TextView
    private lateinit var chipGroupLocationsView: ChipGroup
    private lateinit var layoutLocationsEdit: LinearLayout
    private lateinit var chipGroupLocationsEdit: ChipGroup
    private lateinit var etAddLocation: EditText
    private lateinit var tvPeopleLabel: TextView
    private lateinit var chipGroupPeopleView: ChipGroup
    private lateinit var layoutPeopleEdit: LinearLayout
    private lateinit var chipGroupPeopleEdit: ChipGroup
    private lateinit var etAddPerson: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_result)

        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: ""
        val tags = intent.getStringArrayListExtra(EXTRA_TAGS) ?: arrayListOf()
        val locations = intent.getStringArrayListExtra(EXTRA_LOCATIONS) ?: arrayListOf()
        val people = intent.getStringArrayListExtra(EXTRA_PEOPLE) ?: arrayListOf()
        val fragmentCount = intent.getIntExtra(EXTRA_FRAGMENT_COUNT, 0)

        selectedTags.addAll(tags.filter { it in ALL_TAGS })
        locationList.addAll(locations.distinct().filter { it.isNotBlank() })
        peopleList.addAll(people.distinct().filter { it.isNotBlank() })

        chipGroupTagsView = findViewById(R.id.chip_group_tags_view)
        chipGroupTagsEdit = findViewById(R.id.chip_group_tags_edit)
        tvLocationLabel = findViewById(R.id.tv_location_label)
        chipGroupLocationsView = findViewById(R.id.chip_group_locations_view)
        layoutLocationsEdit = findViewById(R.id.layout_locations_edit)
        chipGroupLocationsEdit = findViewById(R.id.chip_group_locations_edit)
        etAddLocation = findViewById(R.id.et_add_location)
        tvPeopleLabel = findViewById(R.id.tv_people_label)
        chipGroupPeopleView = findViewById(R.id.chip_group_people_view)
        layoutPeopleEdit = findViewById(R.id.layout_people_edit)
        chipGroupPeopleEdit = findViewById(R.id.chip_group_people_edit)
        etAddPerson = findViewById(R.id.et_add_person)

        // 날짜
        findViewById<TextView>(R.id.tv_result_date).text = formatDateLabel(date)

        // AI 생성 텍스트
        val etSummary = findViewById<EditText>(R.id.et_summary)
        etSummary.setText(summary)

        // 조각 수
        findViewById<TextView>(R.id.tv_fragment_count).text = "${fragmentCount}개의 기억 조각으로 만들어졌어요"

        // 수정 버튼
        val btnEdit = findViewById<ImageButton>(R.id.btn_edit)
        btnEdit.setOnClickListener {
            isEditMode = !isEditMode
            updateEditMode(etSummary, btnEdit)
        }

        // 취소 버튼
        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }

        // 저장 버튼
        findViewById<LinearLayout>(R.id.btn_save).setOnClickListener {
            Toast.makeText(this, "기억이 저장되었습니다", Toast.LENGTH_SHORT).show()
            // TODO: 실제 저장 로직 연결
            finish()
        }

        // 위치 추가
        etAddLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addLocation(); true } else false
        }
        findViewById<View>(R.id.btn_add_location).setOnClickListener { addLocation() }

        // 사람 추가
        etAddPerson.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addPerson(); true } else false
        }
        findViewById<View>(R.id.btn_add_person).setOnClickListener { addPerson() }

        renderAll()
    }

    private fun updateEditMode(etSummary: EditText, btnEdit: ImageButton) {
        etSummary.isEnabled = isEditMode
        if (isEditMode) {
            etSummary.requestFocus()
            etSummary.setSelection(etSummary.text.length)
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSummary, InputMethodManager.SHOW_IMPLICIT)
            btnEdit.setBackgroundResource(R.drawable.bg_circle_sub200)
        } else {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etSummary.windowToken, 0)
            btnEdit.setBackgroundResource(android.R.color.transparent)
        }
        renderAll()
    }

    private fun renderAll() {
        renderTags()
        renderLocations()
        renderPeople()
    }

    private fun renderTags() {
        if (isEditMode) {
            chipGroupTagsView.visibility = View.GONE
            chipGroupTagsEdit.visibility = View.VISIBLE
            chipGroupTagsEdit.removeAllViews()
            ALL_TAGS.forEach { tag ->
                chipGroupTagsEdit.addView(createToggleTagChip(tag, tag in selectedTags))
            }
        } else {
            chipGroupTagsEdit.visibility = View.GONE
            chipGroupTagsView.visibility = View.VISIBLE
            chipGroupTagsView.removeAllViews()
            selectedTags.forEach { tag ->
                chipGroupTagsView.addView(createStaticChip("#$tag", R.color.sub_100, R.color.sub_200))
            }
        }
    }

    private fun renderLocations() {
        if (locationList.isEmpty() && !isEditMode) {
            tvLocationLabel.visibility = View.GONE
            chipGroupLocationsView.visibility = View.GONE
            layoutLocationsEdit.visibility = View.GONE
            return
        }
        tvLocationLabel.visibility = View.VISIBLE
        if (isEditMode) {
            chipGroupLocationsView.visibility = View.GONE
            layoutLocationsEdit.visibility = View.VISIBLE
            chipGroupLocationsEdit.removeAllViews()
            locationList.forEach { loc ->
                chipGroupLocationsEdit.addView(createDeletableChip("@ $loc", R.color.main_100, R.color.main_200) {
                    locationList.remove(loc)
                    renderLocations()
                })
            }
        } else {
            layoutLocationsEdit.visibility = View.GONE
            chipGroupLocationsView.visibility = View.VISIBLE
            chipGroupLocationsView.removeAllViews()
            locationList.forEach { loc ->
                chipGroupLocationsView.addView(createStaticChip("@ $loc", R.color.main_100, R.color.main_200))
            }
        }
    }

    private fun renderPeople() {
        if (peopleList.isEmpty() && !isEditMode) {
            tvPeopleLabel.visibility = View.GONE
            chipGroupPeopleView.visibility = View.GONE
            layoutPeopleEdit.visibility = View.GONE
            return
        }
        tvPeopleLabel.visibility = View.VISIBLE
        if (isEditMode) {
            chipGroupPeopleView.visibility = View.GONE
            layoutPeopleEdit.visibility = View.VISIBLE
            chipGroupPeopleEdit.removeAllViews()
            peopleList.forEach { person ->
                chipGroupPeopleEdit.addView(createDeletableChip("$person ×", R.color.sub_100, R.color.sub_200) {
                    peopleList.remove(person)
                    renderPeople()
                })
            }
        } else {
            layoutPeopleEdit.visibility = View.GONE
            chipGroupPeopleView.visibility = View.VISIBLE
            chipGroupPeopleView.removeAllViews()
            peopleList.forEach { person ->
                chipGroupPeopleView.addView(createStaticChip(person, R.color.sub_100, R.color.sub_200))
            }
        }
    }

    private fun addLocation() {
        val text = etAddLocation.text.toString().trim()
        if (text.isNotEmpty() && !locationList.contains(text)) {
            locationList.add(text)
            etAddLocation.text.clear()
            renderLocations()
        }
    }

    private fun addPerson() {
        val text = etAddPerson.text.toString().trim()
        if (text.isNotEmpty() && !peopleList.contains(text)) {
            peopleList.add(text)
            etAddPerson.text.clear()
            renderPeople()
        }
    }

    private fun createToggleTagChip(tag: String, isSelected: Boolean): Chip {
        return Chip(this).apply {
            text = "#$tag"
            isCheckable = true
            isChecked = isSelected
            chipStrokeWidth = 0f
            textSize = 12f
            if (isSelected) {
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MemoryResultActivity, R.color.sub_200)
                )
                setTextColor(ContextCompat.getColor(this@MemoryResultActivity, R.color.brown_50))
            } else {
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MemoryResultActivity, R.color.sub_100)
                )
                setTextColor(ContextCompat.getColor(this@MemoryResultActivity, R.color.sub_200))
            }
            setOnCheckedChangeListener { _, checked ->
                if (checked) selectedTags.add(tag) else selectedTags.remove(tag)
                if (checked) {
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(this@MemoryResultActivity, R.color.sub_200)
                    )
                    setTextColor(ContextCompat.getColor(this@MemoryResultActivity, R.color.brown_50))
                } else {
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(this@MemoryResultActivity, R.color.sub_100)
                    )
                    setTextColor(ContextCompat.getColor(this@MemoryResultActivity, R.color.sub_200))
                }
            }
        }
    }

    private fun createStaticChip(text: String, bgColor: Int, textColor: Int): Chip {
        return Chip(this).apply {
            this.text = text
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(this@MemoryResultActivity, bgColor)
            )
            setTextColor(ContextCompat.getColor(this@MemoryResultActivity, textColor))
            chipStrokeWidth = 0f
            isClickable = false
            isCheckable = false
            textSize = 12f
        }
    }

    private fun createDeletableChip(text: String, bgColor: Int, textColor: Int, onDelete: () -> Unit): Chip {
        return Chip(this).apply {
            this.text = text
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(this@MemoryResultActivity, bgColor)
            )
            setTextColor(ContextCompat.getColor(this@MemoryResultActivity, textColor))
            chipStrokeWidth = 0f
            isClickable = true
            isCheckable = false
            textSize = 12f
            setOnClickListener { onDelete() }
        }
    }

    private fun formatDateLabel(date: String): String = try {
        val ld = LocalDate.parse(date)
        val dow = when (ld.dayOfWeek) {
            DayOfWeek.MONDAY -> "월요일"
            DayOfWeek.TUESDAY -> "화요일"
            DayOfWeek.WEDNESDAY -> "수요일"
            DayOfWeek.THURSDAY -> "목요일"
            DayOfWeek.FRIDAY -> "금요일"
            DayOfWeek.SATURDAY -> "토요일"
            DayOfWeek.SUNDAY -> "일요일"
            else -> ""
        }
        "${ld.year}년 ${ld.monthValue}월 ${ld.dayOfMonth}일 $dow"
    } catch (e: Exception) { date }

    companion object {
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_TAGS = "extra_tags"
        const val EXTRA_LOCATIONS = "extra_locations"
        const val EXTRA_PEOPLE = "extra_people"
        const val EXTRA_FRAGMENT_COUNT = "extra_fragment_count"
    }
}
