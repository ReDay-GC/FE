package com.example.reday.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 데이터 로딩 중 자동으로 loadingView를 show/hide 처리한다.
 * 코루틴이 시작될 때 VISIBLE, 완료(또는 예외)되면 GONE으로 설정된다.
 */
fun Fragment.launchWithLoading(
    loadingView: View,
    block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        loadingView.visibility = View.VISIBLE
        try {
            block()
        } finally {
            loadingView.visibility = View.GONE
        }
    }
}

/**
 * SwipeRefreshLayout의 새로고침 스피너를 자동으로 종료한다.
 * setOnRefreshListener 안에서 사용한다.
 */
fun Fragment.launchWithRefresh(
    swipeRefresh: SwipeRefreshLayout,
    block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            block()
        } finally {
            swipeRefresh.isRefreshing = false
        }
    }
}

fun AppCompatActivity.launchWithLoading(
    loadingView: View,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        loadingView.visibility = View.VISIBLE
        try {
            block()
        } finally {
            loadingView.visibility = View.GONE
        }
    }
}
