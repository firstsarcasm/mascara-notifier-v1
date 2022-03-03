package org.mascara.notifier.service

import org.json.JSONArray
import org.json.JSONObject
import org.mascara.notifier.HttpClient
import org.mascara.notifier.scheduler.MascaraScheduler
import org.mascara.notifier.utils.TimeUtils
import java.time.LocalDate

object MascaraService {
    public fun getSchedule(firstname: String, date: String): String {
        val employeeData = HttpClient.client.getEmployeesData(firstname)

        val employeeId = employeeData.getInt("id")
        val allUsersScheduleInfo = HttpClient.client.getScheduleInfo(date, employeeId.toString())
        val userSchedule = if (allUsersScheduleInfo.has(employeeId.toString())) {
            allUsersScheduleInfo.getJSONArray(employeeId.toString())
        } else {
            JSONArray()
        }

        if (userSchedule.length() == 0) {
            return "выходной!"
        }
        val first = userSchedule.get(0) as JSONObject

        if (first.getString("startTime") == "10:00" && first.getString("endTime") == "22:00") {
            return "записей нет("
        }

        return userSchedule.toString()
    }

    public fun getScheduleForToday(firstname: String, today: LocalDate = TimeUtils.getMoscowNow()): String = getScheduleForTodayPlus(0, firstname, today)

    public fun getScheduleForTomorrow(firstname: String, today: LocalDate = TimeUtils.getMoscowNow()): String = getScheduleForTodayPlus(1, firstname, today)

    public fun getScheduleForDayAfterTomorrow(firstname: String, today: LocalDate = TimeUtils.getMoscowNow()): String = getScheduleForTodayPlus(2, firstname, today)

    private fun getScheduleForTodayPlus(daysCount: Long, firstname: String, today: LocalDate): String {
        val day = today.plusDays(daysCount).toString()
        return getSchedule(firstname, day)
    }

}