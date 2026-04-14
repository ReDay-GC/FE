package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class InquiryWriteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inquiry_write, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_send_inquiry).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.et_inquiry_title).text.toString().trim()
            val content = view.findViewById<EditText>(R.id.et_inquiry_content).text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "문의 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "문의가 접수되었습니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
