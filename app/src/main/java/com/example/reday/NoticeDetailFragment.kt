package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class NoticeDetailFragment : Fragment() {

    companion object {
        const val ARG_TITLE = "notice_title"
        const val ARG_DATE = "notice_date"
        const val ARG_CONTENT = "notice_content"
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

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val date = arguments?.getString(ARG_DATE) ?: ""
        val content = arguments?.getString(ARG_CONTENT) ?: ""

        view.findViewById<TextView>(R.id.tv_toolbar_title).text = title
        view.findViewById<TextView>(R.id.tv_notice_date).text = date
        view.findViewById<TextView>(R.id.tv_notice_content).text = content

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
