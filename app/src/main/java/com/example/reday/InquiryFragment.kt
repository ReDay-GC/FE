package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InquiryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inquiry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_inquiry_write).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, InquiryWriteFragment())
                .addToBackStack(null)
                .commit()
        }

        val items = listOf(
            InquiryUiModel(
                id = 1,
                userName = "사용자",
                date = "2024. 11. 29.",
                title = "기억 생성 오류 문의",
                content = "기억 생성 버튼을 눌러도 AI 생성이 시작되지 않습니다.",
                answer = null
            ),
            InquiryUiModel(
                id = 2,
                userName = "사용자",
                date = "2024. 11. 29.",
                title = "앱 사용 방법 문의",
                content = "기록 조각을 추가하는 방법을 알고 싶습니다.",
                answer = InquiryAnswer(
                    authorName = "Re:Day 팀",
                    date = "2024. 11. 29.",
                    content = "안녕하세요! 홈 화면 하단의 + 버튼을 눌러 기록 조각을 추가하실 수 있습니다."
                )
            )
        )

        val rvInquiries = view.findViewById<RecyclerView>(R.id.rv_inquiries)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty)

        if (items.isEmpty()) {
            rvInquiries.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvInquiries.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            rvInquiries.layoutManager = LinearLayoutManager(requireContext())
            rvInquiries.adapter = InquiryAdapter(items)
        }
    }
}
