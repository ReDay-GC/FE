package com.example.reday

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddMemoryActivity : AppCompatActivity(),
    DateSelectFragment.DateSelectListener,
    AddMemoryFragment.AddMemoryListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_memory)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { onBack() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DateSelectFragment())
                .commit()
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

    // AddMemoryFragment → 뒤로 (step 1으로)
    override fun onBack() {
        findViewById<TextView>(R.id.tv_selected_date).visibility = View.INVISIBLE
        findViewById<ImageButton>(R.id.btn_back).visibility = View.GONE
        updateStepIndicator(step = 1)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DateSelectFragment())
            .commit()
    }

    // AddMemoryFragment → 저장 완료
    override fun onSaved() {
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
