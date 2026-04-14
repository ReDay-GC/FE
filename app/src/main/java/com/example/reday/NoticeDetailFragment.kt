package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NoticeDetailFragment : Fragment() {

    companion object {
        const val ARG_NOTICE_ID = "notice_id"
        const val ARG_TITLE = "notice_title"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notice_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val noticeId = arguments?.getLong(ARG_NOTICE_ID) ?: -1L
        val title = arguments?.getString(ARG_TITLE) ?: ""

        val tvToolbarTitle = view.findViewById<TextView>(R.id.tv_toolbar_title)
        val tvDate = view.findViewById<TextView>(R.id.tv_notice_date)
        val tvContent = view.findViewById<TextView>(R.id.tv_notice_content)

        tvToolbarTitle.text = title

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.noticeApi.getNoticeDetail(noticeId)
                val data = response.data
                tvToolbarTitle.text = data.title
                tvDate.text = formatDate(data.createdAt)
                tvContent.text = data.content
            } catch (_: Exception) {
                tvDate.text = ""
                tvContent.text = "공지사항을 불러오지 못했습니다."
            }
        }
    }

    private fun formatDate(dateTime: String): String {
        return try {
            val parsed = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            "${parsed.year}. ${parsed.monthValue.toString().padStart(2, '0')}. ${parsed.dayOfMonth.toString().padStart(2, '0')}."
        } catch (_: Exception) {
            dateTime.take(10)
        }
    }
}
