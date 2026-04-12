package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.LoginRequest
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var tvSignup: TextView
    private lateinit var progressBar: ProgressBar

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        tvSignup = findViewById(R.id.tv_signup)
        progressBar = findViewById(R.id.progress_bar)

        setupTextWatchers()
        setupPasswordToggle()
        setupClickListeners()
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val enabled = etEmail.text.isNotEmpty() && etPassword.text.isNotEmpty()
                btnLogin.isEnabled = enabled
                btnLogin.alpha = if (enabled) 1.0f else 0.5f
            }
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
    }

    private fun setupPasswordToggle() {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            performLogin(email, password)
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun performLogin(email: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.login(LoginRequest(email, password))
                TokenManager.saveToken(this@LoginActivity, response.data.accessToken)
                RetrofitClient.accessToken = response.data.accessToken
                setLoading(false)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: HttpException) {
                setLoading(false)
                when (e.code()) {
                    400 -> Toast.makeText(this@LoginActivity, "이메일 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this@LoginActivity, "로그인에 실패했습니다. (${e.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@LoginActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
    }
}
