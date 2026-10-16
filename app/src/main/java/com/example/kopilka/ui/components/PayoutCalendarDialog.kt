package com.example.kopilka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText2
import com.example.kopilka.ui.theme.ColorText3
import com.example.kopilka.utils.DeadlineForecast
import com.example.kopilka.utils.formatDate
import com.example.kopilka.utils.formatFullAmount

@Composable
fun DepositInfoRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ColorText3, fontSize = 13.sp)
        Text(
            value,
            color = if (highlight) ColorAccent else ColorText2,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PayoutCalendarDialog(deposit: BankDeposit, endDate: java.time.LocalDate, onDismiss: () -> Unit) {
    val schedule = remember(deposit, endDate) { DeadlineForecast.depositSchedule(deposit, endDate) }
    val totalMonths = java.time.temporal.ChronoUnit.MONTHS.between(deposit.startDate, endDate)
    val monthFormatter = remember {
        java.time.format.DateTimeFormatter.ofPattern("LLL yyyy", java.util.Locale("ru", "RU"))
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorCard, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Календарь выплат", color = ColorText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            DepositInfoRow("Срок", "${formatDate(deposit.startDate)} – ${formatDate(endDate)} \n ($totalMonths мес.)")
            DepositInfoRow("Ставка", "${deposit.interestRate}% годовых", highlight = true)
            DepositInfoRow("Начальная сумма", formatFullAmount(deposit.initialAmount))

            Spacer(Modifier.height(12.dp))

            // Заголовок таблицы
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("Месяц", color = ColorText3, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Начислено", color = ColorText3, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text("Итого", color = ColorText3, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(ColorBorder))

            if (schedule.isEmpty()) {
                Text(
                    "До даты прогноза выплат не будет",
                    color = ColorText3,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    schedule.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text(
                                p.date.format(monthFormatter).replaceFirstChar { it.uppercase() },
                                color = ColorText2, fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "+${formatFullAmount(p.accrued)}",
                                color = ColorAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                formatFullAmount(p.balance),
                                color = ColorText, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Закрыть",
                color = ColorAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}