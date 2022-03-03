package org.mascara.notifier

import khttp.get
import org.json.JSONObject

class HttpClient {
    fun getEmployeesData(firstname: String): JSONObject {
        return get(getEmployeesDataUrl())
                .jsonObject.getJSONArray("employees")
                .find { s -> (s as JSONObject).getString("firstname") == firstname}
                as JSONObject
    }

    fun getScheduleInfo(date: String, employeeId: String): JSONObject {
        return get(getScheduleInfoUrl(date, employeeId))
                .jsonObject.getJSONObject("employees")
    }

    companion object {
        public val client = HttpClient()
        fun getEmployeesDataUrl() = "https://app.arnica.pro/booking/employee/getEmployeesData?mobileApp=false&networkid=&organizationID=10916"
        fun getScheduleInfoUrl(date: String, employeeId: String) = "https://app.arnica.pro/booking/booking/getScheduleInfo/orgid/10916/date/$date?mobileApp=false&id=$employeeId"
    }
}
