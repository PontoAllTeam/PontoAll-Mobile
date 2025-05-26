package com.pontoall.pontoallmobile.domain.model

import android.location.Location

data class MarkPoint(val id: Int, val date: String, val time: String, val location: Location, val userId: Int)