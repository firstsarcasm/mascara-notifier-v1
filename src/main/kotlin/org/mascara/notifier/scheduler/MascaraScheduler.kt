package org.mascara.notifier.scheduler

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mascara.notifier.bot.Listener
import org.mascara.notifier.bot.ScheduleHolder
import org.mascara.notifier.jdbc.DatabaseClient
import org.mascara.notifier.service.MascaraService
import org.mascara.notifier.utils.TimeUtils
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

object MascaraScheduler {
    private val log = Logger.getLogger(MascaraScheduler::class.java.name)

    private val map = ConcurrentHashMap<Long, Listener>()

    @Volatile
    private var schedulerToday: LocalDate = TimeUtils.getMoscowNow()

    private val todayDbScheduleId = DatabaseClient.Schedule.DayIds.TODAY.dayId
    private val tomorrowDbScheduleId = DatabaseClient.Schedule.DayIds.TOMORROW.dayId
    private val afterTwoDaysDbScheduleId = DatabaseClient.Schedule.DayIds.AFTER_TWO_DAYS.dayId

    public fun subscribe(telegramChatId: Long, listener: Listener) {
        DatabaseClient.Subscribers.insertIfNotExists(telegramChatId)
        map[telegramChatId] = listener
    }

    public fun unsubscribe(telegramChatId: Long) {
        DatabaseClient.Subscribers.deleteIfExists(telegramChatId)
        map.remove(telegramChatId)
    }

    public fun run(listener: Listener) {
        syncSubscribers(listener)
        Scheduler.schedule(Runnable {
            try {
                listenMascaraSource()
            } catch (e: Exception) {
                log.warning("Something went wrong: $e")
            }
        })
    }

    @Synchronized
    private fun listenMascaraSource() {
        val now = TimeUtils.getMoscowNow()

        val todayResultFromMascara = MascaraService.getScheduleForToday("Даша С", now)
        val tomorrowResultFromMascara = MascaraService.getScheduleForTomorrow("Даша С", now)
        val afterTwoDaysResultFromMascara = MascaraService.getScheduleForDayAfterTomorrow("Даша С", now)

        if (schedulerToday.isBefore(now)) {
            val holder = ScheduleHolder(todayResultFromMascara, tomorrowResultFromMascara, afterTwoDaysResultFromMascara)
            map.forEach {
                it.value.onNewDay(it.key, holder)
            }

            transaction {
                DatabaseClient.Schedule.update(todayDbScheduleId, todayResultFromMascara)
            }
            transaction {
                DatabaseClient.Schedule.update(tomorrowDbScheduleId, tomorrowResultFromMascara)
            }
            transaction {
                DatabaseClient.Schedule.update(afterTwoDaysDbScheduleId, afterTwoDaysResultFromMascara)
            }
            schedulerToday = now
            return
        }

        val scheduleList = transaction {
            DatabaseClient.Schedule.selectAll().toList()
        }

        if (scheduleList.isEmpty()) {
            transaction {
                DatabaseClient.Schedule.insert(todayDbScheduleId, todayResultFromMascara)
                DatabaseClient.Schedule.insert(tomorrowDbScheduleId, tomorrowResultFromMascara)
                DatabaseClient.Schedule.insert(afterTwoDaysDbScheduleId, afterTwoDaysResultFromMascara)
            }
            return
        } else {
            val todayDbResult = transaction { DatabaseClient.Schedule.selectBy(todayDbScheduleId) }
            val tomorrowDbResult = transaction { DatabaseClient.Schedule.selectBy(tomorrowDbScheduleId) }
            val afterTwoDaysDbResult = transaction { DatabaseClient.Schedule.selectBy(afterTwoDaysDbScheduleId) }

            todayResultFromMascara.setAsCurrentIfChanged(
                    todayDbResult, todayDbScheduleId, "сегодня")

            tomorrowResultFromMascara.setAsCurrentIfChanged(
                    tomorrowDbResult, tomorrowDbScheduleId, "завтра")

            afterTwoDaysResultFromMascara.setAsCurrentIfChanged(
                    afterTwoDaysDbResult, afterTwoDaysDbScheduleId, "послезавтра")
        }
        return
    }

    private fun syncSubscribers(listener: Listener) {
        DatabaseClient.Subscribers
                .getAllChatIds()
                .forEach { telegramChatId ->
                    map.put(telegramChatId, listener)
                }
    }

    private fun String.setAsCurrentIfChanged(resultFromDb: String, id: Long, dayDescription: String) {
        val resultFromMascara = this
        if (resultFromMascara != resultFromDb) {
            log.info("something was changed.\nMascara result: $resultFromMascara\n db result: $resultFromDb")
            notifyUsersWithSubscription(resultFromMascara, dayDescription)
            transaction { DatabaseClient.Schedule.update(id, resultFromMascara) }
        }
    }

    private fun notifyUsersWithSubscription(resultFromMascara: String, dayDescription: String) {
        map.forEach { (t: Long, u: Listener) ->
            u.onValueChanged(t, resultFromMascara, dayDescription)
        }
    }
}