package com.pontoall.pontoallmobile

import com.google.gson.annotations.SerializedName

data class TimeRecordRequest(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("date") val date: String, // Formato: "yyyy-MM-dd"
    @SerializedName("time") val time: String, // Formato: "HH:mm:ss"
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("justification") val justification: String? = null,
    @SerializedName("userId") val userId: Int,
    @SerializedName("photo") val photo: String = "",
    @SerializedName("dailyRecordId") val dailyRecordId: Int = 0,
    @SerializedName("workScheduleId") val workScheduleId: Int
)