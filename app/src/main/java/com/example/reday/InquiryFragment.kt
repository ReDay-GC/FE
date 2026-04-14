package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.reday.data.remote.InquiryListDto
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.utils.TokenManager
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

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

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val rvInquiries = view.findViewById<RecyclerView>(R.id.rv_inquiries)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty)

        swipeRefresh.setColorSchemeResources(R.color.main_200)

        fun loadInquiries() {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.inquiryApi.getInquiries()
                    val userName = TokenManager.getUserName(requireContext()).ifBlank { "사용자" }
                    val items = response.data.map { it.toUiModel(userName) }

                    if (items.isEmpty()) {
                        rvInquiries.visibility = View.GONE
                        layoutEmpty.visibility = View.VISIBLE
                    } else {
                        rvInquiries.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE
                        rvInquiries.layoutManager = LinearLayoutManager(requireContext())
                        rvInquiries.adapter = InquiryAdapter(items) { inquiry ->
                            val fragment = InquiryDetailFragment().apply {
                                arguments = Bundle().apply {
                                    putLong(InquiryDetailFragment.ARG_INQUIRY_ID, inquiry.id)
                                    putString(InquiryDetailFragment.ARG_TITLE, inquiry.title)
                                }
                            }
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.content_container, fragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                } catch (_: Exception) {
                    rvInquiries.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } finally {
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        swipeRefresh.setOnRefreshListener { loadInquiries() }
        loadInquiries()
    }

    private fun InquiryListDto.toUiModel(userName: String): InquiryUiModel {
        val answer = if (status == "ANSWERED" && replyContent != null) {
            InquiryAnswer(
                authorName = "Re:Day 팀",
                date = "",
                content = replyContent
            )
        } else null

        return InquiryUiModel(
            id = inquiryId,
            userName = userName,
            date = formatDate(createdAt),
            title = title,
            content = contentPreview,
            answer = answer
        )
    }

    private fun formatDate(dateTime: String): String {
        return try {
            val parsed = ZonedDateTime.parse(dateTime).toLocalDate()
            "${parsed.year}. ${parsed.monthValue.toString().padStart(2, '0')}. ${parsed.dayOfMonth.toString().padStart(2, '0')}."
        } catch (_: Exception) {
            dateTime.take(10)
        }
    }
}
