package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Calendar

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTodayDate(view)
        setupAddMemoryButton(view)
    }

    private fun setupAddMemoryButton(view: View) {
        view.findViewById<View>(R.id.btn_add_memory).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.content_container, DateSelectFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setTodayDate(view: View) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = arrayOf("일", "월", "화", "수", "목", "금", "토")[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        view.findViewById<TextView>(R.id.tv_date).text = "${year}년 ${month}월 ${day}일 ${dayOfWeek}요일"
    }
}
