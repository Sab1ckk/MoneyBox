package com.example.kopilka.utils

import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.data.entity.Deposit
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.pow


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
         * Баланс самого вклада через [monthsSinceStart] месяцев после даты открытия.
         * Проценты начисляются ТОЛЬКО на сумму вклада (initialAmount), пополнения
         * копилки в расчёте процентов не участвуют. Капитализация сложная: раз в
         * capitalizationMonths месяцев начисленные проценты добавляются к телу вклада.
         * Считаются только полностью завершённые периоды, отсчёт идёт от startDate.
         */
        private fun bankBalance(deposit: BankDeposit, monthsSinceStart: Long): Double {
            val capMonths = deposit.capitalizationMonths.coerceAtLeast(1)
            val periodRate = deposit.interestRate / 100.0 / 12.0 * capMonths
            val completedPeriods = (monthsSinceStart.coerceAtLeast(0) / capMonths).toInt()
            return deposit.initialAmount * (1 + periodRate).pow(completedPeriods)
        }

        /**
         * Накоплено через [months] месяцев от сегодняшнего дня:
         * пополнения (без процентов) + ежемесячный взнос * months + баланс вклада.
         *
         * savedAmount уже включает initialAmount (см. calcSavedAmount), поэтому
         * часть "только пополнения" = savedAmount - initialAmount.
         */
        private fun simulate(
            savedAmount: Double,
            monthly: Double,
            deposit: BankDeposit,
            months: Long,
            today: LocalDate
        ): Double {
            val contributions = savedAmount - deposit.initialAmount
            val monthsSinceOpening = ChronoUnit.MONTHS.between(deposit.startDate, today) + months
            return contributions + monthly * months + bankBalance(deposit, monthsSinceOpening)
        }

        /**
         * Без вклада: remaining / avgMonthly, округляем вверх до целого месяца.
         * С вкладом: помесячная симуляция — к балансу каждый месяц прибавляется
         * средний взнос, а на каждый период капитализации (capitalizationMonths) —
         * проценты на стартовую сумму вклада (initialAmount).
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

            var months = 1L
            while (months <= MAX_MONTHS &&
                simulate(savedAmount, avgMonthly, bankDeposit, months, today) < targetAmount
            ) months++

            if (months > MAX_MONTHS) return CannotEstimate
            return Estimated(today.plusMonths(months), avgMonthly)
        }

        /**
         * Когда дедлайн задан ВРУЧНУЮ: сколько нужно откладывать каждый день,
         * чтобы успеть накопить цель к этому сроку. Если подключён банковский
         * вклад — проценты, начисляются только на его сумму и к дедлайну,
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

            if (bankDeposit == null) {
                return RequiredDailyAmount.Required(remaining / daysLeft, daysLeft)
            }

            val months = ChronoUnit.MONTHS.between(today, deadline).coerceAtLeast(1)

            // Бинарный поиск минимального ежемесячного взноса, при котором баланс
            // с процентами достигнет цели к дедлайну.
            var lo = 0.0
            var hi = remaining / months
            repeat(50) {
                val mid = (lo + hi) / 2
                if (simulate(savedAmount, mid, bankDeposit, months, today) >= targetAmount) hi = mid else lo = mid
            }
            val amountPerDay = hi * months / daysLeft
            return RequiredDailyAmount.Required(amountPerDay = amountPerDay, daysLeft = daysLeft)
        }

        fun calcSavedAmount(deposits: List<Deposit>, bankDeposit: BankDeposit?): Double =
            deposits.sumOf { it.amount } + (bankDeposit?.initialAmount ?: 0.0)
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