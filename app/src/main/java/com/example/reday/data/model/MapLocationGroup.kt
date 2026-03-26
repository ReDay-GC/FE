package com.example.reday.data.model

data class MapLocationGroup(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val totalFragmentCount: Int,
    val memories: List<MemoryUiModel>
)
