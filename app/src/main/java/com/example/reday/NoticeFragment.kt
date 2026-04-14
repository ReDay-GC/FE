package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.remote.NoticeListDto
import com.example.reday.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

        val rvNotices = view.findViewById<RecyclerView>(R.id.rv_notices)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.noticeApi.getNotices()
                val items = response.data.map { it.toUiModel() }

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
                                putLong(NoticeDetailFragment.ARG_NOTICE_ID, notice.id)
                                putString(NoticeDetailFragment.ARG_TITLE, notice.title)
                            }
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.content_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            } catch (_: Exception) {
                rvNotices.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun NoticeListDto.toUiModel(): NoticeUiModel {
        return NoticeUiModel(
            id = noticeId,
            title = title,
            preview = contentPreview,
            time = formatDate(createdAt),
            isRead = !isNew,
            date = formatDate(createdAt),
            content = ""
        )
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
