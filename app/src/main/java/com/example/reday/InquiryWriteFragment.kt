package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.InquiryCreateRequest
import com.example.reday.data.remote.RetrofitClient
import kotlinx.coroutines.launch

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

        val etTitle = view.findViewById<EditText>(R.id.et_inquiry_title)
        val etContent = view.findViewById<EditText>(R.id.et_inquiry_content)
        val btnSend = view.findViewById<View>(R.id.btn_send_inquiry)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSend.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "문의 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    RetrofitClient.inquiryApi.createInquiry(InquiryCreateRequest(title, content))
                    Toast.makeText(requireContext(), "문의가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "문의 전송에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    btnSend.isEnabled = true
                }
            }
        }
    }
}
