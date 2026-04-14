package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TermsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val content = arguments?.getString(ARG_CONTENT) ?: ""

        view.findViewById<TextView>(R.id.tv_toolbar_title).text = title
        view.findViewById<TextView>(R.id.tv_terms_content).text = content

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"

        private const val TERMS_TEXT = """제1조 (목적)
본 약관은 Re:Day 서비스(이하 "서비스")의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임 사항을 규정합니다.

제2조 (서비스 이용)
이용자는 본 약관에 동의하고 서비스에 가입함으로써 서비스를 이용할 수 있습니다.

제3조 (계정 관리)
이용자는 본인의 계정 정보를 안전하게 관리할 책임이 있으며, 타인에게 양도하거나 공유할 수 없습니다.

제4조 (서비스 이용 제한)
회사는 이용자가 본 약관을 위반하거나 서비스의 정상적인 운영을 방해하는 경우 서비스 이용을 제한할 수 있습니다.

제5조 (면책조항)
회사는 천재지변, 서비스 장애 등 불가피한 사유로 인한 서비스 중단에 대해 책임을 지지 않습니다."""

        private const val PRIVACY_TEXT = """개인정보 처리방침

1. 수집하는 개인정보 항목
Re:Day는 서비스 제공을 위해 다음 개인정보를 수집합니다.
- 이름(닉네임), 이메일 주소

2. 개인정보 수집 목적
- 회원 가입 및 서비스 제공
- 기억 기록 저장 및 관리

3. 개인정보 보유 기간
회원 탈퇴 시까지 보유하며, 탈퇴 후 지체 없이 파기합니다.

4. 개인정보 제3자 제공
이용자의 동의 없이 개인정보를 제3자에게 제공하지 않습니다.

5. 개인정보 처리 위탁
서비스 운영을 위해 필요한 경우 최소한의 범위 내에서 위탁할 수 있으며, 위탁 시 이용자에게 고지합니다.

6. 문의처
개인정보 관련 문의: support@reday.com"""

        fun newTerms() = TermsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, "서비스 이용 약관")
                putString(ARG_CONTENT, TERMS_TEXT)
            }
        }

        fun newPrivacy() = TermsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, "개인정보 처리방침")
                putString(ARG_CONTENT, PRIVACY_TEXT)
            }
        }
    }
}
