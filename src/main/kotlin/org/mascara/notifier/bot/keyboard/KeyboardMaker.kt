package org.mascara.notifier.bot.keyboard

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

object KeyboardMaker {
    private val keyboard = createKeyboard()
    const val scheduleButton = "Расписание"
    const val subscribeButton = "Подписаться"
    const val unsubscribeButton = "Отписаться"

    private val rawKeyboardMessage = SendMessage()
            .setReplyMarkup(keyboard)
            .setText("------------------")

    public fun getKeyboardMessage(chatId: Long): SendMessage {
        return rawKeyboardMessage.setChatId(chatId)
    }

    private fun createKeyboard(): ReplyKeyboardMarkup {
        val row = KeyboardRow()
        val row2 = KeyboardRow()
        row.add(KeyboardButton().apply { text = scheduleButton })
        row2.add(KeyboardButton().apply { text = subscribeButton })
        row2.add(KeyboardButton().apply { text = unsubscribeButton })
        val keyboard = ReplyKeyboardMarkup().apply {
            keyboard = listOf(row, row2)
            resizeKeyboard = true
        }
        return keyboard
    }
}