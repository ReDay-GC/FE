package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.UpdateNameRequest
import com.example.reday.utils.TokenManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserJoined: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
        tvUserJoined = view.findViewById(R.id.tv_user_joined)

        // 로컬 캐시로 먼저 표시 후 API로 갱신
        bindLocalUserInfo()
        loadUserProfile()

        // 이름 편집
        view.findViewById<View>(R.id.btn_edit_name).setOnClickListener {
            showEditNameDialog()
        }

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
                .setPositiveButton("로그아웃") { _, _ -> performLogout() }
                .setNegativeButton("취소", null)
                .show()
        }

        // 회원 탈퇴
        view.findViewById<MaterialCardView>(R.id.item_withdraw).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("탈퇴하면 모든 기억과 기록이 삭제됩니다.\n정말 탈퇴하시겠어요?")
                .setPositiveButton("탈퇴") { _, _ -> performWithdraw() }
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

    private fun bindLocalUserInfo() {
        val name = TokenManager.getUserName(requireContext())
        val email = TokenManager.getUserEmail(requireContext())
        tvUserName.text = name.ifBlank { "이름 없음" }
        tvUserEmail.text = email.ifBlank { "-" }
        tvUserJoined.visibility = View.GONE
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.userApi.getMyProfile()
                val data = response.data
                tvUserName.text = data.name
                tvUserEmail.text = data.email
                tvUserJoined.text = "가입일: ${formatDate(data.createdAt)}"
                tvUserJoined.visibility = View.VISIBLE
                TokenManager.saveUserName(requireContext(), data.name)
                TokenManager.saveUserEmail(requireContext(), data.email)
            } catch (_: Exception) {
                // 로컬 캐시 유지, 별도 에러 표시 없음
            }
        }
    }

    private fun showEditNameDialog() {
        val input = EditText(requireContext()).apply {
            setText(tvUserName.text)
            hint = "새 이름을 입력해주세요"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("이름 변경")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(requireContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    updateName(newName)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateName(newName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.userApi.updateMyProfile(UpdateNameRequest(newName))
                tvUserName.text = response.data.name
                TokenManager.saveUserName(requireContext(), response.data.name)
                Toast.makeText(requireContext(), "이름이 변경되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "이름 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDate(dateTime: String): String {
        return try {
            val parsed = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            "${parsed.year}.${parsed.monthValue.toString().padStart(2, '0')}.${parsed.dayOfMonth.toString().padStart(2, '0')}"
        } catch (_: Exception) {
            dateTime.take(10).replace("-", ".")
        }
    }

    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.authApi.logout()
            } catch (_: Exception) {
                // 서버 오류와 무관하게 로컬 토큰 삭제 후 이동
            } finally {
                TokenManager.clearToken(requireContext())
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun performWithdraw() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.withdraw()
                if (response.isSuccessful) {
                    TokenManager.clearToken(requireContext())
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "탈퇴 처리에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
