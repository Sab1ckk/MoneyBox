// Путь в проекте: app/src/main/java/com/example/kopilka/utils/DeadlineForecast.kt

package com.example.kopilka.utils

import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.data.entity.Deposit
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * Прогноз даты завершения цели, когда пользователь НЕ указал дедлайн вручную.
 *
 * Принцип (см. постановку задачи):
 * 1. Дедлайн задан пользователем → эта логика не используется вообще.
 * 2. Дедлайн не задан:
 *    - пополнений ещё не было → показывать нечего (NoContributionsYet);
 *    - есть пополнения → считаем средний темп в ₽/месяц от даты первого
 *      пополнения до сегодня и экстраполируем: "если продолжите откладывать
 *      столько же в месяц — накопите цель к дате X";
 *    - если подключён банковский вклад — на баланс дополнительно капает
 *      процент на стартовую сумму вклада на каждый период капитализации
 *      (который и так задан в месяцах — capitalizationMonths).
 */
sealed class DeadlineForecast {
    /** Пополнений ещё не было — строить прогноз не на чем. */
    object NoContributionsYet : DeadlineForecast()

    /** Цель уже накоплена. */
    object AlreadyReached : DeadlineForecast()

    /** Средний темп пополнений <= 0 — оценить дату нельзя. */
    object CannotEstimate : DeadlineForecast()

    /** Расчётная дата + средний темп пополнений (₽/месяц), которым он получен. */
    data class Estimated(val date: LocalDate, val avgMonthlyAmount: Double) : DeadlineForecast()

    companion object {

        private const val AVG_DAYS_PER_MONTH = 30.44 // календарный месяц в среднем
        private const val MAX_MONTHS = 120L // ~10 лет — защита от бесконечного цикла симуляции

        /**
         * Средняя сумма пополнений в месяц: сумма всех взносов, делённая на
         * число месяцев от даты первого взноса до сегодня (минимум 1 месяц,
         * чтобы единственное пополнение "сегодня" не давало нереалистично
         * быстрый прогноз за счёт деления на ~0).
         */
        fun avgMonthlyAmount(deposits: List<Deposit>, today: LocalDate = LocalDate.now()): Double? {
            if (deposits.isEmpty()) return null
            val earliest = deposits.minOf { it.date }
            val daysElapsed = ChronoUnit.DAYS.between(earliest, today).coerceAtLeast(1)
            val monthsElapsed = (daysElapsed / AVG_DAYS_PER_MONTH).coerceAtLeast(1.0)
            val total = deposits.sumOf { it.amount }
            return total / monthsElapsed
        }

        /**
         * Без вклада: remaining / avgMonthly, округляем вверх до целого месяца.
         * С вкладом: помесячная симуляция — к балансу каждый месяц прибавляется
         * средний взнос, а на каждый период капитализации (capitalizationMonths) —
         * проценты на стартовую сумму вклада (initialAmount), как и в исходном прототипе.
         */
        fun estimate(
            savedAmount: Double,
            targetAmount: Double,
            deposits: List<Deposit>,
            bankDeposit: BankDeposit?,
            today: LocalDate = LocalDate.now()
        ): DeadlineForecast {
            val remaining = targetAmount - savedAmount
            if (remaining <= 0) return AlreadyReached

            val avgMonthly = avgMonthlyAmount(deposits, today) ?: return NoContributionsYet
            if (avgMonthly <= 0) return CannotEstimate

            if (bankDeposit == null) {
                val monthsNeeded = ceil(remaining / avgMonthly).toLong()
                return Estimated(today.plusMonths(monthsNeeded), avgMonthly)
            }

            val capitalizationMonths = bankDeposit.capitalizationMonths.coerceAtLeast(1)
            val periodsPerYear = 12.0 / capitalizationMonths
            val periodRate = (bankDeposit.interestRate / 100.0) / periodsPerYear

            var balance = savedAmount
            var monthsPassed = 0L
            while (balance < targetAmount && monthsPassed < MAX_MONTHS) {
                monthsPassed++
                balance += avgMonthly
                if (monthsPassed % capitalizationMonths == 0L) {
                    balance += bankDeposit.initialAmount * periodRate
                }
            }
            return Estimated(today.plusMonths(monthsPassed), avgMonthly)
        }

        /**
         * Когда дедлайн задан ВРУЧНУЮ: сколько нужно откладывать каждый день,
         * чтобы успеть накопить цель к этому сроку. Если подключён банковский
         * вклад — проценты, которые накапают на initialAmount к дедлайну,
         * уменьшают необходимую сумму пополнений.
         */
        fun requiredDailyAmount(
            savedAmount: Double,
            targetAmount: Double,
            deadline: LocalDate,
            bankDeposit: BankDeposit?,
            today: LocalDate = LocalDate.now()
        ): RequiredDailyAmount {
            val remaining = targetAmount - savedAmount
            if (remaining <= 0) return RequiredDailyAmount.AlreadyReached

            val daysLeft = ChronoUnit.DAYS.between(today, deadline)
            if (daysLeft <= 0) return RequiredDailyAmount.DeadlinePassed

            val projectedInterest = bankDeposit?.let { deposit ->
                val capitalizationMonths = deposit.capitalizationMonths.coerceAtLeast(1)
                val monthsUntilDeadline = ChronoUnit.MONTHS.between(today, deadline)
                val completedPeriods = monthsUntilDeadline / capitalizationMonths
                if (completedPeriods <= 0) {
                    0.0
                } else {
                    val periodsPerYear = 12.0 / capitalizationMonths
                    val periodRate = (deposit.interestRate / 100.0) / periodsPerYear
                    deposit.initialAmount * periodRate * completedPeriods
                }
            } ?: 0.0

            val remainingAfterInterest = (remaining - projectedInterest).coerceAtLeast(0.0)
            val amountPerDay = remainingAfterInterest / daysLeft

            return RequiredDailyAmount.Required(amountPerDay = amountPerDay, daysLeft = daysLeft)
        }
    }
}

/** Результат расчёта requiredDailyAmount() — сумма в день для ручного дедлайна. */
sealed class RequiredDailyAmount {
    /** Цель уже накоплена — вносить больше не нужно. */
    object AlreadyReached : RequiredDailyAmount()

    /** Дедлайн уже наступил или в прошлом — считать сумму в день не имеет смысла. */
    object DeadlinePassed : RequiredDailyAmount()

    data class Required(val amountPerDay: Double, val daysLeft: Long) : RequiredDailyAmount()
}