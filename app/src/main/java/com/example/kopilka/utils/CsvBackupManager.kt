package com.example.kopilka.utils

import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Deposit
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.entity.GoalCategory
import com.example.kopilka.data.repository.BankDepositRepository
import com.example.kopilka.data.repository.CategoryRepository
import com.example.kopilka.data.repository.DepositRepository
import com.example.kopilka.data.repository.GoalCategoryRepository
import com.example.kopilka.data.repository.GoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Экспорт/импорт всей базы данных «Копилки» в один CSV-файл с секциями.
 *
 * Формат: перед каждой таблицей идёт маркер "# SECTION:<ИМЯ>", затем строка
 * заголовка колонок, затем сами строки данных:
 *
 *   # SECTION:GOALS
 *   id,name,description,targetAmount,deadline,createdDate,isCompleted
 *   1,"Поездка в Японию","...",350000.0,19814,19740,false
 *   # SECTION:CATEGORIES
 *   ...
 *
 * Импорт ПОЛНОСТЬЮ заменяет текущее содержимое базы содержимым файла —
 * это резервная копия/восстановление, а не слияние данных.
 */
@Singleton
class CsvBackupManager @Inject constructor(
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository,
    private val goalCategoryRepository: GoalCategoryRepository,
    private val depositRepository: DepositRepository,
    private val bankDepositRepository: BankDepositRepository
) {

    sealed class ImportResult {
        data class Success(
            val goals: Int,
            val categories: Int,
            val deposits: Int,
            val bankDeposits: Int
        ) : ImportResult()

        data class Error(val message: String) : ImportResult()
    }

    suspend fun exportTo(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val goals = goalRepository.getAllOnce()
        val categories = categoryRepository.getAllOnce()
        val goalCategories = goalCategoryRepository.getAllOnce()
        val deposits = depositRepository.getAllOnce()
        val bankDeposits = bankDepositRepository.getAllOnce()

        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.appendLine("# SECTION:GOALS")
            writer.appendLine("id,name,description,targetAmount,deadline,createdDate,isCompleted")
            goals.forEach { g ->
                writer.appendLine(
                    listOf(
                        g.id, quote(g.name), quote(g.description), g.targetAmount,
                        g.deadline?.toEpochDay(), g.createdDate.toEpochDay(), g.isCompleted
                    ).joinToString(",")
                )
            }

            writer.appendLine("# SECTION:CATEGORIES")
            writer.appendLine("id,name,icon")
            categories.forEach { c ->
                writer.appendLine(listOf(c.id, quote(c.name), quote(c.icon)).joinToString(","))
            }

            writer.appendLine("# SECTION:GOAL_CATEGORIES")
            writer.appendLine("goalId,categoryId")
            goalCategories.forEach { gc -> writer.appendLine("${gc.goalId},${gc.categoryId}") }

            writer.appendLine("# SECTION:DEPOSITS")
            writer.appendLine("id,goalId,amount,date,comment")
            deposits.forEach { d ->
                writer.appendLine(
                    listOf(d.id, d.goalId, d.amount, d.date.toEpochDay(), quote(d.comment)).joinToString(",")
                )
            }

            writer.appendLine("# SECTION:BANK_DEPOSITS")
            writer.appendLine("id,goalId,interestRate,capitalizationMonths,startDate,initialAmount")
            bankDeposits.forEach { b ->
                writer.appendLine(
                    listOf(
                        b.id, b.goalId, b.interestRate, b.capitalizationMonths,
                        b.startDate.toEpochDay(), b.initialAmount
                    ).joinToString(",")
                )
            }
        }
    }

    suspend fun importFrom(inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        try {
            val lines = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readLines()

            val goals = mutableListOf<Goal>()
            val categories = mutableListOf<Category>()
            val goalCategories = mutableListOf<GoalCategory>()
            val deposits = mutableListOf<Deposit>()
            val bankDeposits = mutableListOf<BankDeposit>()

            var section = ""
            var skipHeader = false

            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("# SECTION:")) {
                    section = line.removePrefix("# SECTION:").trim()
                    skipHeader = true
                    continue
                }
                if (skipHeader) {
                    skipHeader = false
                    continue
                }

                val cols = splitCsvLine(line)
                when (section) {
                    "GOALS" -> goals += Goal(
                        id = cols[0].toInt(),
                        name = cols[1],
                        description = cols[2],
                        targetAmount = cols[3].toDouble(),
                        deadline = LocalDate.ofEpochDay(cols[4].toLong()),
                        createdDate = LocalDate.ofEpochDay(cols[5].toLong()),
                        isCompleted = cols[6].toBoolean()
                    )
                    "CATEGORIES" -> categories += Category(
                        id = cols[0].toInt(),
                        name = cols[1],
                        icon = cols[2],
                        color = "gray"
                    )
                    "GOAL_CATEGORIES" -> goalCategories += GoalCategory(
                        goalId = cols[0].toInt(),
                        categoryId = cols[1].toInt()
                    )
                    "DEPOSITS" -> deposits += Deposit(
                        id = cols[0].toInt(),
                        goalId = cols[1].toInt(),
                        amount = cols[2].toDouble(),
                        date = LocalDate.ofEpochDay(cols[3].toLong()),
                        comment = cols[4]
                    )
                    "BANK_DEPOSITS" -> bankDeposits += BankDeposit(
                        id = cols[0].toInt(),
                        goalId = cols[1].toInt(),
                        interestRate = cols[2].toDouble(),
                        capitalizationMonths = cols[3].toInt(),
                        startDate = LocalDate.ofEpochDay(cols[4].toLong()),
                        initialAmount = cols[5].toDouble()
                    )
                }
            }

            if (goals.isEmpty()) {
                return@withContext ImportResult.Error(
                    "Файл не похож на резервную копию Копилки — не найдено ни одной цели"
                )
            }

            // Полностью заменяем текущие данные содержимым файла.
            // Каскадное удаление Goal/Category само очистит deposits,
            // bank_deposits и goal_categories.
            goalRepository.deleteAll()
            categoryRepository.deleteAll()

            categories.forEach { categoryRepository.insert(it) }
            goals.forEach { goalRepository.insert(it) }
            goalCategories.forEach { goalCategoryRepository.insert(it) }
            deposits.forEach { depositRepository.insert(it) }
            bankDeposits.forEach { bankDepositRepository.insert(it) }

            ImportResult.Success(
                goals = goals.size,
                categories = categories.size,
                deposits = deposits.size,
                bankDeposits = bankDeposits.size
            )
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Не удалось прочитать файл — он повреждён или имеет неверный формат")
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        goalRepository.deleteAll()
        categoryRepository.deleteAll()
    }

    private fun quote(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    /** Простой разбор CSV-строки с поддержкой кавычек и запятых внутри полей. */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result += current.toString()
        return result
    }
}
