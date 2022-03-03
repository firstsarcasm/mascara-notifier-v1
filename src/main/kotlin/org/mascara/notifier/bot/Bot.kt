package org.mascara.notifier.bot

import org.mascara.notifier.bot.keyboard.KeyboardMaker
import org.mascara.notifier.dto.model.StringTimePeriod
import org.mascara.notifier.dto.model.TimePeriod
import org.mascara.notifier.scheduler.MascaraScheduler
import org.mascara.notifier.service.MascaraService
import org.mascara.notifier.utils.JacksonUtils.deserialize
import org.mascara.notifier.utils.StringUtils.containsOr
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.time.LocalTime

private const val EMPLOYEE_NAME = "NAME"

class Bot : TelegramLongPollingBot(), Listener {

    override fun onValueChanged(chatId: Long, newValue: String, dayDescription: String) {
        sendSchedule(newValue, chatId, "Расписание на $dayDescription изменилось, теперь оно такое:")
    }

    override fun onNewDay(chatId: Long, scheduleHolder: ScheduleHolder) {
        sendTextMessage(chatId, "Начался новый день, расписание:\n")
        sendSchedule(scheduleHolder.todaySchedule, chatId, "Сегодня:")
        sendSchedule(scheduleHolder.tomorrowSchedule, chatId, "Завтра:")
        sendSchedule(scheduleHolder.afterTwoDaysSchedule, chatId, "Послезавтра:")
    }

    override fun onUpdateReceived(update: Update) {
        val chatId = update.extractChatId()
        sendMainKeyboard(chatId)

        if (update.hasTextMessage()) {
            if (update.message.text.startsWith(KeyboardMaker.subscribeButton)) {
                MascaraScheduler.subscribe(chatId, this)
                sendTextMessage(chatId, "Вы подписаны на обновления")
                return
            }

            if (update.message.text.startsWith(KeyboardMaker.unsubscribeButton)) {
                MascaraScheduler.unsubscribe(chatId)
                sendTextMessage(chatId, "Вы отписаны от обновлений")
                return
            }

            sendScheduleForThreeDays(chatId)
        }
    }

    private fun sendScheduleForThreeDays(chatId: Long) {
        val todaySchedule = MascaraService.getScheduleForToday(EMPLOYEE_NAME)
        val tomorrowSchedule = MascaraService.getScheduleForTomorrow(EMPLOYEE_NAME)
        val afterTwoDaysSchedule = MascaraService.getScheduleForDayAfterTomorrow(EMPLOYEE_NAME)

        sendSchedule(todaySchedule, chatId, "Сегодня:")
        sendSchedule(tomorrowSchedule, chatId, "Завтра:")
        sendSchedule(afterTwoDaysSchedule, chatId, "Послезавтра:")
    }

    private fun sendSchedule(schedule: String, chatId: Long, prefixString: String) {
        if (schedule.containsOr("{", "}", "[", "]")) {
            val sb = StringBuilder()
            getWorkTimeListFromFreeTime(schedule).forEach {
                sb.appendln("Запись: ${it.startTime} - ${it.endTime}")
            }
            sendTextMessage(chatId, "$prefixString \n$sb")
        } else {
            sendTextMessage(chatId, "$prefixString $schedule")
        }
    }

    private fun getWorkTimeListFromFreeTime(scheduleJson: String): List<TimePeriod> {
        val startOfWork = LocalTime.of(10, 0)
        val endOfWork = LocalTime.of(22, 0)
        val result = ArrayList<TimePeriod>()
        val deserialize = scheduleJson.deserialize(Array<StringTimePeriod>::class.java)
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

    private fun tryExecute(message: SendMessage) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun tryExecute(message: SendDocument) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    override fun getBotUsername(): String = ""
    override fun getBotToken(): String = ""

    private fun sendTextMessage(chatId: Long, text: String) {
        val message = SendMessage()
                .setText(text)
                .setChatId(chatId)
        tryExecute(message)
    }

    private fun sendMainKeyboard(chatId: Long) {
        tryExecute(KeyboardMaker.getKeyboardMessage(chatId))
    }

    private fun sendFile(chatId: Long, file: File) {
        val message = SendDocument().setChatId(chatId).setDocument(file)
        tryExecute(message)
    }

    private fun Update.hasTextMessage() = this.hasMessage() && this.message.hasText()

    private fun Update.extractChatId() =
            if (this.hasTextMessage()) message.chatId
            else callbackQuery.message.chatId
}


