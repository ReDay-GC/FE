package com.example.reday

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddMemoryActivity : AppCompatActivity(),
    DateSelectFragment.DateSelectListener,
    AddMemoryFragment.AddMemoryListener {

    companion object {
        const val EXTRA_YEAR = "extra_year"
        const val EXTRA_MONTH = "extra_month"
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_GO_TO_TIMELINE = "extra_go_to_timeline"
        // 수정 모드
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
        const val EXTRA_EDIT_LOCAL_ID = "extra_edit_local_id"
        const val EXTRA_EDIT_SERVER_ID = "extra_edit_server_id"
        const val EXTRA_EDIT_FRAGMENT_TYPE = "extra_edit_fragment_type"
        const val EXTRA_EDIT_CONTENT_TEXT = "extra_edit_content_text"
        const val EXTRA_EDIT_PHOTO_URL = "extra_edit_photo_url"
        const val EXTRA_EDIT_VOICE_URL = "extra_edit_voice_url"
        const val EXTRA_EDIT_DURATION_SEC = "extra_edit_duration_sec"
        const val EXTRA_EDIT_CREATED_AT = "extra_edit_created_at"
        const val EXTRA_EDIT_DATE = "extra_edit_date"
        const val EXTRA_EDIT_LOCATION_NAME = "extra_edit_location_name"
        const val EXTRA_EDIT_LATITUDE = "extra_edit_latitude"
        const val EXTRA_EDIT_LONGITUDE = "extra_edit_longitude"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_memory)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { handleExit() }
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { handleExit() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleExit() }
        })

        if (savedInstanceState == null) {
            val isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
            val year = intent.getIntExtra(EXTRA_YEAR, -1)
            val month = intent.getIntExtra(EXTRA_MONTH, -1)
            val day = intent.getIntExtra(EXTRA_DAY, -1)
            if (isEditMode && year != -1 && month != -1 && day != -1) {
                showEditModeFragment(year, month, day)
            } else if (year != -1 && month != -1 && day != -1) {
                onDateSelected(year, month, day)
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, DateSelectFragment())
                    .commit()
            }
        }
    }

    // DateSelectFragment → 날짜 선택 완료
    override fun onDateSelected(year: Int, month: Int, day: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day)
        val dayOfWeekStr = arrayOf("일", "월", "화", "수", "목", "금", "토")[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]

        val tvDate = findViewById<TextView>(R.id.tv_selected_date)
        tvDate.text = "${year}년 ${month}월 ${day}일 ${dayOfWeekStr}요일"
        tvDate.visibility = View.VISIBLE

        updateStepIndicator(step = 2)
        findViewById<ImageButton>(R.id.btn_back).visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AddMemoryFragment.newInstance(year, month, day))
            .commit()
    }

    private fun handleExit() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is AddMemoryFragment && fragment.hasUnsavedContent()) {
            showExitConfirmDialog()
        } else {
            finish()
        }
    }

    private fun showExitConfirmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_confirm, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<android.widget.TextView>(R.id.btn_dialog_cancel)
            .setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.TextView>(R.id.btn_dialog_leave)
            .setOnClickListener { dialog.dismiss(); finish() }
        dialog.show()
    }

    // AddMemoryFragment → 뒤로 (step 1으로)
    override fun onBack() {
        findViewById<TextView>(R.id.tv_selected_date).visibility = View.INVISIBLE
        findViewById<ImageButton>(R.id.btn_back).visibility = View.GONE
        updateStepIndicator(step = 1)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DateSelectFragment())
            .commit()
    }

    // 수정 모드: DateSelectFragment 없이 바로 AddMemoryFragment(수정 모드)로 진입
    private fun showEditModeFragment(year: Int, month: Int, day: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day)
        val dayOfWeekStr = arrayOf("일", "월", "화", "수", "목", "금", "토")[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]

        val tvDate = findViewById<TextView>(R.id.tv_selected_date)
        tvDate.text = "${year}년 ${month}월 ${day}일 ${dayOfWeekStr}요일"
        tvDate.visibility = View.VISIBLE
        updateStepIndicator(step = 2)
        findViewById<ImageButton>(R.id.btn_back).visibility = View.GONE

        val fragment = AddMemoryFragment.newInstanceEdit(
            year = year, month = month, day = day,
            localId = intent.getLongExtra(EXTRA_EDIT_LOCAL_ID, -1L),
            serverId = intent.getLongExtra(EXTRA_EDIT_SERVER_ID, -1L).takeIf { it != -1L },
            fragmentType = intent.getStringExtra(EXTRA_EDIT_FRAGMENT_TYPE) ?: "TEXT",
            contentText = intent.getStringExtra(EXTRA_EDIT_CONTENT_TEXT),
            photoUrl = intent.getStringExtra(EXTRA_EDIT_PHOTO_URL),
            voiceUrl = intent.getStringExtra(EXTRA_EDIT_VOICE_URL),
            durationSec = intent.getIntExtra(EXTRA_EDIT_DURATION_SEC, 0),
            createdAt = intent.getStringExtra(EXTRA_EDIT_CREATED_AT) ?: "",
            date = intent.getStringExtra(EXTRA_EDIT_DATE) ?: "",
            locationName = intent.getStringExtra(EXTRA_EDIT_LOCATION_NAME),
            latitude = intent.getDoubleExtra(EXTRA_EDIT_LATITUDE, Double.NaN).takeIf { !it.isNaN() },
            longitude = intent.getDoubleExtra(EXTRA_EDIT_LONGITUDE, Double.NaN).takeIf { !it.isNaN() }
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // AddMemoryFragment → 저장 완료
    override fun onSaved() {
        val isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        if (isEditMode) {
            setResult(android.app.Activity.RESULT_OK)
        }
        finish()
    }

    private fun updateStepIndicator(step: Int) {
        val step1Circle = findViewById<TextView>(R.id.step1_circle)
        val step1Label = findViewById<TextView>(R.id.step1_label)
        val stepDivider = findViewById<View>(R.id.step_divider)
        val step2Circle = findViewById<TextView>(R.id.step2_circle)
        val step2Label = findViewById<TextView>(R.id.step2_label)

        if (step == 1) {
            step1Circle.setBackgroundResource(R.drawable.bg_step_active)
            step1Circle.setTextColor(ContextCompat.getColor(this, R.color.brown_50))
            step1Label.setTextColor(ContextCompat.getColor(this, R.color.brown_700))
            stepDivider.setBackgroundColor(ContextCompat.getColor(this, R.color.inactive))
            step2Circle.setBackgroundResource(R.drawable.bg_step_inactive)
            step2Circle.setTextColor(ContextCompat.getColor(this, R.color.brown_300))
            step2Label.setTextColor(ContextCompat.getColor(this, R.color.brown_300))
        } else {
            step1Circle.setBackgroundResource(R.drawable.bg_step_active)
            step1Circle.setTextColor(ContextCompat.getColor(this, R.color.brown_50))
            step1Label.setTextColor(ContextCompat.getColor(this, R.color.brown_500))
            stepDivider.setBackgroundColor(ContextCompat.getColor(this, R.color.main_200))
            step2Circle.setBackgroundResource(R.drawable.bg_step_active)
            step2Circle.setTextColor(ContextCompat.getColor(this, R.color.brown_50))
            step2Label.setTextColor(ContextCompat.getColor(this, R.color.brown_700))
        }
    }
}
