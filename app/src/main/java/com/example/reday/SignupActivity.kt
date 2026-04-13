package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.data.remote.SignupRequest
import com.example.reday.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SignupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnTogglePasswordConfirm: ImageButton
    private lateinit var cbAll: CheckBox
    private lateinit var cbTerms: CheckBox
    private lateinit var cbPrivacy: CheckBox
    private lateinit var btnSignup: MaterialButton
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    private var isPwdVisible = false
    private var isPwdConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etPasswordConfirm = findViewById(R.id.et_password_confirm)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        btnTogglePasswordConfirm = findViewById(R.id.btn_toggle_password_confirm)
        cbAll = findViewById(R.id.cb_all)
        cbTerms = findViewById(R.id.cb_terms)
        cbPrivacy = findViewById(R.id.cb_privacy)
        btnSignup = findViewById(R.id.btn_signup)
        tvLogin = findViewById(R.id.tv_login)
        progressBar = findViewById(R.id.progress_bar)

        setupTextWatchers()
        setupPasswordToggles()
        setupCheckboxes()
        setupClickListeners()
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updateSignupButtonState()
        }
        etName.addTextChangedListener(watcher)
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        etPasswordConfirm.addTextChangedListener(watcher)
    }

    private fun setupPasswordToggles() {
        btnTogglePassword.setOnClickListener {
            isPwdVisible = !isPwdVisible
            etPassword.transformationMethod = if (isPwdVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            btnTogglePassword.setImageResource(if (isPwdVisible) R.drawable.ic_eye else R.drawable.ic_eye_off)
            etPassword.setSelection(etPassword.text.length)
        }

        btnTogglePasswordConfirm.setOnClickListener {
            isPwdConfirmVisible = !isPwdConfirmVisible
            etPasswordConfirm.transformationMethod = if (isPwdConfirmVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            btnTogglePasswordConfirm.setImageResource(if (isPwdConfirmVisible) R.drawable.ic_eye else R.drawable.ic_eye_off)
            etPasswordConfirm.setSelection(etPasswordConfirm.text.length)
        }
    }

    private fun setupCheckboxes() {
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            cbTerms.isChecked = isChecked
            cbPrivacy.isChecked = isChecked
            updateSignupButtonState()
        }

        val itemWatcher = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            cbAll.setOnCheckedChangeListener(null)
            cbAll.isChecked = cbTerms.isChecked && cbPrivacy.isChecked
            cbAll.setOnCheckedChangeListener { _, isChecked ->
                cbTerms.isChecked = isChecked
                cbPrivacy.isChecked = isChecked
                updateSignupButtonState()
            }
            updateSignupButtonState()
        }
        cbTerms.setOnCheckedChangeListener(itemWatcher)
        cbPrivacy.setOnCheckedChangeListener(itemWatcher)

        // 약관 행 클릭 → 체크박스 토글 (화살표 영역은 별도 처리)
        findViewById<LinearLayout>(R.id.ll_terms).setOnClickListener {
            cbTerms.isChecked = !cbTerms.isChecked
        }
        findViewById<LinearLayout>(R.id.ll_privacy).setOnClickListener {
            cbPrivacy.isChecked = !cbPrivacy.isChecked
        }

        // 화살표 클릭 → 팝업 다이얼로그 (행 클릭 이벤트 전파 차단)
        findViewById<ImageView>(R.id.iv_terms_detail).setOnClickListener {
            showTermsDialog()
        }
        findViewById<ImageView>(R.id.iv_privacy_detail).setOnClickListener {
            showPrivacyDialog()
        }
    }

    private fun showTermsDialog() {
        AlertDialog.Builder(this)
            .setTitle("이용약관")
            .setMessage(TERMS_TEXT)
            .setPositiveButton("동의") { dialog, _ ->
                cbTerms.isChecked = true
                dialog.dismiss()
            }
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(this)
            .setTitle("개인정보 처리방침")
            .setMessage(PRIVACY_TEXT)
            .setPositiveButton("동의") { dialog, _ ->
                cbPrivacy.isChecked = true
                dialog.dismiss()
            }
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateSignupButtonState() {
        val fieldsFilled = etName.text.isNotEmpty()
                && etEmail.text.isNotEmpty()
                && etPassword.text.isNotEmpty()
                && etPasswordConfirm.text.isNotEmpty()
        val agreementsChecked = cbTerms.isChecked && cbPrivacy.isChecked
        val enabled = fieldsFilled && agreementsChecked
        btnSignup.isEnabled = enabled
        btnSignup.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun setupClickListeners() {
        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val passwordConfirm = etPasswordConfirm.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "올바른 이메일 형식을 입력해주세요."
                return@setOnClickListener
            }
            if (password.length < 8) {
                etPassword.error = "비밀번호는 8자 이상이어야 합니다."
                return@setOnClickListener
            }
            if (password != passwordConfirm) {
                etPasswordConfirm.error = "비밀번호가 일치하지 않습니다."
                return@setOnClickListener
            }

            performSignup(name, email, password, passwordConfirm)
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun performSignup(
        name: String,
        email: String,
        password: String,
        passwordConfirm: String
    ) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.signup(
                    SignupRequest(
                        name = name,
                        email = email,
                        password = password,
                        passwordConfirm = passwordConfirm,
                        termsAgreed = cbTerms.isChecked,
                        privacyAgreed = cbPrivacy.isChecked
                    )
                )
                TokenManager.saveUserName(this@SignupActivity, response.data.name)
                TokenManager.saveUserEmail(this@SignupActivity, response.data.email)
                setLoading(false)
                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finish()
            } catch (e: HttpException) {
                setLoading(false)
                when (e.code()) {
                    400 -> Toast.makeText(this@SignupActivity, "입력값을 확인해주세요.", Toast.LENGTH_SHORT).show()
                    409 -> Toast.makeText(this@SignupActivity, "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this@SignupActivity, "회원가입에 실패했습니다. (${e.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@SignupActivity, "네트워크 오류: ${e.javaClass.simpleName} - ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSignup.isEnabled = !loading
    }

    companion object {
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
    }
}
