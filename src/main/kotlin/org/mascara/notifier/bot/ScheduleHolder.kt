package org.mascara.notifier.bot

data class ScheduleHolder(
        val todaySchedule: String,
        val tomorrowSchedule: String,
        val afterTwoDaysSchedule: String
)