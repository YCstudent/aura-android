package com.edistrive.aura.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateFormat {
    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val CHINESE_LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    private val CHINESE_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")

    fun parseIsoDate(value: String?): LocalDate? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return try {
            LocalDate.parse(trimmed.substring(0, minOf(10, trimmed.length)), ISO_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    fun formatIsoDate(date: LocalDate?): String? = date?.format(ISO_DATE)

    fun formatChineseDate(date: LocalDate?): String =
        date?.format(CHINESE_LONG) ?: "-"

    fun formatChineseShort(date: LocalDate?): String =
        date?.format(CHINESE_SHORT) ?: "-"

    fun chineseFromIso(iso: String?): String = formatChineseDate(parseIsoDate(iso))

    fun calcAge(birthIso: String?, today: LocalDate = LocalDate.now()): Int? {
        val birth = parseIsoDate(birthIso) ?: return null
        if (birth.isAfter(today)) return null
        return Period.between(birth, today).years
    }

    fun daysUntil(endIso: String?, today: LocalDate = LocalDate.now()): Int? {
        val end = parseIsoDate(endIso) ?: return null
        return Period.between(today, end).days +
            Period.between(today, end).months * 30 +
            Period.between(today, end).years * 365
    }
}
