package com.example.kopilka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kopilka.ui.theme.CategoryPalette
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorBase
import com.example.kopilka.ui.theme.ColorBorder
import com.example.kopilka.ui.theme.ColorCard
import com.example.kopilka.ui.theme.ColorSurface
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText3

@Composable
fun AddCategoryDialog(
    name: String,
    selectedColorHex: String,
    onNameChange: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .background(ColorCard, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Новая категория",
                color = ColorText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Название
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = ColorText, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(ColorAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text("Название категории", color = ColorText3, fontSize = 15.sp)
                        }
                        inner()
                    }
                )
            }

            Text(
                text = "Цвет",
                color = ColorText3,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                CategoryPalette.PRESET_COLORS.forEach { color ->
                    val hex = CategoryPalette.toHex(color)
                    val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                    ColorSwatch(
                        color = color,
                        isSelected = isSelected,
                        onClick = { onColorSelected(hex) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = ColorText3)
                }
                Button(
                    onClick = onConfirm,
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorAccent,
                        contentColor = ColorBase,
                        disabledContainerColor = ColorSurface,
                        disabledContentColor = ColorText3
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Добавить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(34.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(if (isSelected) 30.dp else 24.dp)
                .background(color, CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = ColorText,
                    shape = CircleShape
                )
        )
    }
}