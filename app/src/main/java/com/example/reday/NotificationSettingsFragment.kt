package com.example.reday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.NotificationSettingsRequest
import com.example.reday.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class NotificationSettingsFragment : Fragment() {

    private lateinit var switchPush: SwitchCompat
    private lateinit var switchUnwritten: SwitchCompat
    private lateinit var switchAi: SwitchCompat

    // 스위치 변경 이벤트 중복 호출 방지
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchPush = view.findViewById(R.id.switch_push)
        switchUnwritten = view.findViewById(R.id.switch_unwritten)
        switchAi = view.findViewById(R.id.switch_ai)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadSettings()
        setupSwitchListeners()
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationApi.getSettings()
                val data = response.data
                isLoading = true
                switchPush.isChecked = data.pushEnabled
                switchUnwritten.isChecked = data.dailyRecordEnabled
                switchAi.isChecked = data.aiGenerationEnabled
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    private fun setupSwitchListeners() {
        val listener = { _: android.widget.CompoundButton, _: Boolean ->
            if (!isLoading) saveSettings()
        }
        switchPush.setOnCheckedChangeListener(listener)
        switchUnwritten.setOnCheckedChangeListener(listener)
        switchAi.setOnCheckedChangeListener(listener)
    }

    private fun saveSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.notificationApi.updateSettings(
                    NotificationSettingsRequest(
                        pushEnabled = switchPush.isChecked,
                        dailyRecordEnabled = switchUnwritten.isChecked,
                        aiGenerationEnabled = switchAi.isChecked
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "설정 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
