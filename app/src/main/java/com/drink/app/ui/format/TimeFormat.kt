package com.drink.app.ui.format

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
fun displayTime(time: LocalTime): String = time.format(timeFormat)
