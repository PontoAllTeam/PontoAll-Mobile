package com.pontoall.pontoallmobile.data.remote.model

data class MarkPointDTO(
    val id: Int,
    val date: String,
    val time: String,
    val location: GeolocationDTO,
    val photo: String,
    val userId: Int
)
