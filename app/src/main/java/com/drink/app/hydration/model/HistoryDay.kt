package com.drink.app.hydration.model

import java.time.LocalDate

data class HistoryDay(val date: LocalDate, val totalMl: Long, val goalMl: Int?)
