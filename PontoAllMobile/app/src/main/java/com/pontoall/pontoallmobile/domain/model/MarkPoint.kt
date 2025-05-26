package com.pontoall.pontoallmobile.domain.model

import com.pontoall.pontoallmobile.domain.contracts.Geolocation

data class MarkPoint(
    val id: Int = 0,
    val date: String,
    val time: String,
    val location: Geolocation,
    val photo: String,
    val userId: Int
)