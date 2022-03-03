package org.mascara.notifier

import org.mascara.notifier.bot.Bot
import org.mascara.notifier.jdbc.DatabaseClient
import org.mascara.notifier.scheduler.MascaraScheduler
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            DatabaseClient.initDatabase()
            val bot = initBot()
            MascaraScheduler.run(bot)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun initBot(): Bot {
        ApiContextInitializer.init()
        return Bot().also { bot ->
            TelegramBotsApi().registerBot(bot)
        }
    }
}