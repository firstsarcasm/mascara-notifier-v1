package org.mascara.notifier

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalTime
import kotlin.test.Test

val nullLocalTime: LocalTime? = null

class DeserializationTest {

    data class C(var startTime: String? = null, var endTime: String? = null) {
        constructor() : this(null, null) {
        }
    }

    data class TimePeriod(var startTime: LocalTime? = null, var endTime: LocalTime? = null) {
        constructor() : this(nullLocalTime, nullLocalTime)
        constructor(start: String?, end: String?) : this(LocalTime.parse(start), LocalTime.parse(end))
    }

    val om = ObjectMapper()
    public fun <E> String.deserialize(clazz: Class<Array<E>>) = om.readValue(this, clazz)
    @Test
    fun s() {
        val s = "[{\"startTime\":\"12:00\",\"endTime\":\"14:00\"}," +
                "{\"startTime\":\"19:00\",\"endTime\":\"21:00\"}]"
        val result = getWorkTimeFromFreeTime(s)
        println(result)

        val s2 = "[{\"startTime\":\"19:00\",\"endTime\":\"21:00\"}]"
        val result2 = getWorkTimeFromFreeTime(s2)
        println(result2)


        val s3 = "[{\"startTime\":\"19:00\",\"endTime\":\"22:00\"}]"
        val result3 = getWorkTimeFromFreeTime(s3)
        println(result3)

    }

    private fun getWorkTimeFromFreeTime(scheduleJson: String): List<TimePeriod> {
        val startOfWork = LocalTime.of(10, 0)
        val endOfWork = LocalTime.of(22, 0)
        val result = ArrayList<TimePeriod>()
        val deserialize = scheduleJson.deserialize(Array<C>::class.java)
                .map { c -> TimePeriod(c.startTime, c.endTime) }
                .sortedBy { it.startTime }
                .toList()

        if (deserialize.first().startTime != startOfWork) {
            result.add(TimePeriod(startOfWork, deserialize.first().startTime))
        }

        for (x in deserialize.indices) {
            val startOfOrder = deserialize.get(x).endTime
            if (startOfOrder == endOfWork) {
                break
            }
            if (x + 1 > deserialize.size - 1) {
                result.add(TimePeriod(startOfOrder, endOfWork))
                break
            }
            val endOfOrder = deserialize.get(x + 1).startTime
            result.add(TimePeriod(startOfOrder, endOfOrder))
        }
        return result
    }

}