package com.example.reday

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.remote.RetrofitClient
import com.example.reday.utils.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        RetrofitClient.accessToken = TokenManager.getToken(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation()
        setupProfileButton()

        // 백스택 변화에 따라 바텀 네비 및 앱 헤더 표시/숨김
        supportFragmentManager.addOnBackStackChangedListener {
            val shouldHide = supportFragmentManager.backStackEntryCount > 0
            val bottomNav = findViewById<View>(R.id.bottom_nav)
            val divider = findViewById<View>(R.id.bottom_nav_divider)
            val appHeader = findViewById<View>(R.id.app_header)
            bottomNav?.visibility = if (shouldHide) View.GONE else View.VISIBLE
            divider?.visibility = if (shouldHide) View.GONE else View.VISIBLE
            appHeader?.visibility = if (shouldHide) View.GONE else View.VISIBLE
            // 알림 화면에서 나올 때 배지 재확인
            if (!shouldHide) updateNotificationBadge()
        }

        updateNotificationBadge()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        if (supportFragmentManager.findFragmentById(R.id.content_container) == null) {
            showFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(HomeFragment())
                    true
                }
                R.id.nav_calendar -> {
                    showFragment(CalendarFragment())
                    true
                }
                R.id.nav_map -> {
                    showFragment(MapFragment())
                    true
                }
                R.id.nav_archive -> {
                    showFragment(ArchiveFragment())
                    true
                }
                R.id.nav_analysis -> {
                    showFragment(AnalysisFragment())
                    true
                }
                else -> false
            }
        }

        // 아이콘-텍스트 간격 4dp 적용
        val paddingPx = (4 * resources.displayMetrics.density + 0.5f).toInt()
        bottomNav.post {
            val menuView = bottomNav.getChildAt(0) as? ViewGroup ?: return@post
            for (i in 0 until menuView.childCount) {
                val item = menuView.getChildAt(i) as? ViewGroup ?: continue
                for (j in 0 until item.childCount) {
                    val child = item.getChildAt(j) as? ViewGroup ?: continue
                    if (child.childCount > 0 && child.getChildAt(0) is TextView) {
                        child.setPadding(child.paddingLeft, paddingPx, child.paddingRight, child.paddingBottom)
                        break
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_HOME, false)) {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_home
        showFragment(HomeFragment())
    }

    private fun setupProfileButton() {
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        findViewById<View>(R.id.btn_notification).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_container, NotificationFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    fun updateNotificationBadge() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationApi.getNotifications("ALL")
                val hasUnread = response.data.any { !it.isRead }
                findViewById<View>(R.id.badge_notification)?.visibility =
                    if (hasUnread) View.VISIBLE else View.GONE
            } catch (_: Exception) {
                // 실패 시 배지 유지
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, fragment)
            .commit()
    }

    companion object {
        const val EXTRA_NAVIGATE_HOME = "extra_navigate_home"
    }
}
