package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.reday.utils.TokenManager
import com.google.android.material.card.MaterialCardView

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 유저 정보 바인딩
        val name = TokenManager.getUserName(requireContext())
        val email = TokenManager.getUserEmail(requireContext())

        view.findViewById<TextView>(R.id.tv_user_name).text = name.ifBlank { "이름 없음" }
        view.findViewById<TextView>(R.id.tv_user_email).text = email.ifBlank { "-" }
        view.findViewById<TextView>(R.id.tv_user_joined).visibility =
            if (name.isBlank() && email.isBlank()) android.view.View.GONE else android.view.View.VISIBLE

        // 뒤로 가기
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 푸시 알림
        view.findViewById<MaterialCardView>(R.id.item_push_notification).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, NotificationSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        // 공지사항
        view.findViewById<MaterialCardView>(R.id.item_notice).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, NoticeFragment())
                .addToBackStack(null)
                .commit()
        }

        // 문의하기
        view.findViewById<MaterialCardView>(R.id.item_inquiry).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, InquiryFragment())
                .addToBackStack(null)
                .commit()
        }

        // 로그아웃
        view.findViewById<MaterialCardView>(R.id.item_logout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠어요?")
                .setPositiveButton("로그아웃") { _, _ ->
                    TokenManager.clearToken(requireContext())
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 회원 탈퇴
        view.findViewById<MaterialCardView>(R.id.item_withdraw).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("탈퇴하면 모든 기억과 기록이 삭제됩니다.\n정말 탈퇴하시겠어요?")
                .setPositiveButton("탈퇴") { _, _ ->
                    Toast.makeText(requireContext(), "회원 탈퇴 준비 중입니다.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 서비스 이용 약관
        view.findViewById<MaterialCardView>(R.id.item_terms).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, TermsFragment.newTerms())
                .addToBackStack(null)
                .commit()
        }

        // 개인정보 처리방침
        view.findViewById<MaterialCardView>(R.id.item_privacy).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_container, TermsFragment.newPrivacy())
                .addToBackStack(null)
                .commit()
        }
    }
}
