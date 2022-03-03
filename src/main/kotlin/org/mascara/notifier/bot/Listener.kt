package org.mascara.notifier.bot

interface Listener {
    fun onValueChanged(chatId: Long, newValue: String, dayDescription: String = "сегодня")
    fun onNewDay(chatId: Long, scheduleHolder: ScheduleHolder)
}