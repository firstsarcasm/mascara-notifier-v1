package org.mascara.notifier.jdbc

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.net.URISyntaxException
import java.sql.SQLException


object DatabaseClient {
    public fun initDatabase() {
        getConnection()
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Schedule)
            SchemaUtils.createMissingTablesAndColumns(Subscribers)
        }
    }

    object Schedule : Table() {
        val id = long("id").primaryKey()
        val schedule = varchar("schedule", length = 100000)

        public enum class DayIds(val dayId: Long) {
            TODAY(1),
            TOMORROW(2),
            AFTER_TWO_DAYS(3)
        }

        public fun selectBy(rowId: Long): String {
            return Schedule.select { id eq rowId }.first().get(Schedule.schedule)
        }

        public fun update(scheduleId: Long, newValue: String) {
            Schedule.update({ id eq scheduleId }) {
                it[schedule] = newValue
            }
        }

        public fun insert(scheduleId: Long, value: String) {
            Schedule.insert {
                it[id] = scheduleId
                it[schedule] = value
            }
        }
    }

    /**
     * Including auto transactions
     */
    object Subscribers : Table() {
        val chatId = long("chat_id").primaryKey()

        private fun selectBy(telegramChatId: Long): ResultRow? {
            return Subscribers.select {
                Subscribers.chatId.eq(telegramChatId)
            }.firstOrNull()
        }

        public fun getAllChatIds() = transaction {
            Subscribers.selectAll().map { row -> row[Subscribers.chatId] }
        }

        public fun insertIfNotExists(telegramChatId: Long) {
            transaction {
                val subscriber = Subscribers.selectBy(telegramChatId)
                if (subscriber == null) {
                    Subscribers.insert {
                        it[Subscribers.chatId] = telegramChatId
                    }
                }
            }
        }

        public fun deleteIfExists(telegramChatId: Long) {
            transaction {
                val subscriber = Subscribers.select { Subscribers.chatId.eq(telegramChatId) }.firstOrNull()

                if (subscriber != null) {
                    Subscribers.deleteWhere {
                        Subscribers.chatId.eq(telegramChatId)
                    }
                }
            }
        }
    }

    @Throws(URISyntaxException::class, SQLException::class)
    private fun getConnection(): Database {
        val dbUri = URI(System.getenv("DATABASE_URL").trim())

        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path

        return Database.connect(dbUrl, "org.postgresql.Driver", username, password)
    }
}