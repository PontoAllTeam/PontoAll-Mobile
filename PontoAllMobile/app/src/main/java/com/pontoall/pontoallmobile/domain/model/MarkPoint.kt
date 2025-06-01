package com.pontoall.pontoallmobile.domain.model

data class MarkPoint(
    val id: Int,
    val date: String,
    val time: String,
    val location: Geolocation,
    val photo: String,
    val userId: Int
)