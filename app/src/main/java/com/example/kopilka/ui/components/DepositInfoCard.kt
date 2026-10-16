package com.example.kopilka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.ui.components.DepositInfoCard
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.utils.formatDate
import com.example.kopilka.utils.formatFullAmount

@Composable
fun DepositInfoCard(deposit: BankDeposit, onDismiss: () -> Unit) {
    val capLabel = when (deposit.capitalizationMonths) {
        1 -> "Ежемесячно"
        3 -> "Ежеквартально"
        12 -> "Ежегодно"
        else -> "Раз в ${deposit.capitalizationMonths} мес."
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorCard, RoundedCornerShape(20.dp))
                .border(1.dp, ColorBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Text(
                "Банковский вклад",
                color = ColorText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            DepositInfoRow(
                "Ставка",
                "${formatRate(deposit.interestRate)}% годовых",
                highlight = true
            )
            DepositInfoRow("Капитализация", capLabel)
            DepositInfoRow("Сумма вклада", formatFullAmount(deposit.initialAmount))
            DepositInfoRow("Дата открытия", formatDate(deposit.startDate))
        }}

}

private fun formatRate(rate: Double): String =
    if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()