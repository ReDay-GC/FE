package com.example.reday.utils

fun String?.toEmotionEmoji(): String? = when {
    this.isNullOrBlank() -> null
    contains("즐거") || contains("HAPPY")   -> "😊"
    contains("설레") || contains("EXCITED") -> "🥰"
    contains("평온") || contains("CONTENT") -> "😌"
    contains("신나") || contains("EXCITED") -> "🤩"
    contains("지친") -> "😩"
    contains("힘든") || contains("SAD")     -> "😢"
    contains("평범") || contains("NEUTRAL") -> "😐"
    else -> null
}
