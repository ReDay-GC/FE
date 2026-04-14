package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NoticeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val items = listOf(
            NoticeUiModel(
                id = 1,
                title = "12월 업데이트 안내",
                preview = "새로운 기능이 추가되었습니다! 기억 카드 꾸미기 기능을 확인해보세요.",
                time = "5분 전",
                isRead = false,
                date = "2024. 12. 01.  16:00",
                content = "안녕하세요, Re:Day입니다.\n\n12월 업데이트 내용을 안내드립니다.\n\n1. 기억 카드 꾸미기 기능 추가\n기억 카드에 스티커와 배경을 추가할 수 있는 기능이 추가되었습니다.\n\n2. 지도 기능 개선\n기억의 위치 정보를 더 정확하게 표시하도록 개선되었습니다.\n\n3. 성능 향상\n앱 전반의 로딩 속도가 개선되었습니다.\n\n이용해 주셔서 감사합니다."
            ),
            NoticeUiModel(
                id = 2,
                title = "서비스 업데이트 안내",
                preview = "이번 달부터 AI 기억 생성 기능이 더욱 향상되었습니다.",
                time = "1일 전",
                isRead = false,
                date = "2024. 11. 30.  10:00",
                content = "안녕하세요, Re:Day입니다.\n\nAI 기억 생성 기능이 업그레이드되었습니다.\n\n- 더욱 자연스러운 기억 제목 생성\n- 감정 분석 기능 추가\n- 태그 자동 추천 개선\n\n앞으로도 더 좋은 서비스로 찾아오겠습니다."
            ),
            NoticeUiModel(
                id = 3,
                title = "서비스 점검 완료 안내",
                preview = "11월 25일 새벽 2시~4시 진행된 서비스 점검이 완료되었습니다.",
                time = "5일 전",
                isRead = true,
                date = "2024. 11. 25.  04:00",
                content = "안녕하세요, Re:Day입니다.\n\n11월 25일 새벽 2시~4시에 진행된 정기 서비스 점검이 완료되었습니다.\n\n점검 내용:\n- 서버 안정성 강화\n- 데이터베이스 최적화\n- 보안 패치 적용\n\n이용에 불편을 드려 죄송합니다.\n앞으로도 안정적인 서비스를 제공하기 위해 최선을 다하겠습니다."
            )
        )

        val rvNotices = view.findViewById<RecyclerView>(R.id.rv_notices)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty)

        if (items.isEmpty()) {
            rvNotices.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvNotices.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            rvNotices.layoutManager = LinearLayoutManager(requireContext())
            rvNotices.adapter = NoticeAdapter(items) { notice ->
                val fragment = NoticeDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(NoticeDetailFragment.ARG_TITLE, notice.title)
                        putString(NoticeDetailFragment.ARG_DATE, notice.date)
                        putString(NoticeDetailFragment.ARG_CONTENT, notice.content)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.content_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
