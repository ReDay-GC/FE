package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.RetrofitClient
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InquiryDetailFragment : Fragment() {

    companion object {
        const val ARG_INQUIRY_ID = "inquiry_id"
        const val ARG_TITLE = "inquiry_title"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inquiry_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inquiryId = arguments?.getLong(ARG_INQUIRY_ID) ?: -1L
        val titlePreview = arguments?.getString(ARG_TITLE) ?: ""

        val tvTitle = view.findViewById<TextView>(R.id.tv_inquiry_title)
        val tvDate = view.findViewById<TextView>(R.id.tv_inquiry_date)
        val tvContent = view.findViewById<TextView>(R.id.tv_inquiry_content)
        val layoutPending = view.findViewById<LinearLayout>(R.id.layout_answer_pending)
        val layoutReceived = view.findViewById<MaterialCardView>(R.id.layout_answer_received)
        val tvAnswerAuthor = view.findViewById<TextView>(R.id.tv_answer_author)
        val tvAnswerDate = view.findViewById<TextView>(R.id.tv_answer_date)
        val tvAnswerContent = view.findViewById<TextView>(R.id.tv_answer_content)

        tvTitle.text = titlePreview

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.inquiryApi.getInquiryDetail(inquiryId)
                val data = response.data

                tvTitle.text = data.title
                tvDate.text = formatDate(data.createdAt)
                tvContent.text = data.content

                if (data.status == "ANSWERED" && data.replyContent != null) {
                    layoutPending.visibility = View.GONE
                    layoutReceived.visibility = View.VISIBLE
                    tvAnswerAuthor.text = "Re:Day 팀"
                    tvAnswerDate.text = data.repliedAt?.let { formatDate(it) } ?: ""
                    tvAnswerContent.text = data.replyContent
                } else {
                    layoutPending.visibility = View.VISIBLE
                    layoutReceived.visibility = View.GONE
                }
            } catch (_: Exception) {
                tvContent.text = "문의 내용을 불러오지 못했습니다."
                layoutPending.visibility = View.VISIBLE
                layoutReceived.visibility = View.GONE
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
