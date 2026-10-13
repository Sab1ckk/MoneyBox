package com.example.kopilka.ui.screens.pin

import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import com.example.kopilka.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kopilka.ui.theme.ColorAccent
import com.example.kopilka.ui.theme.ColorAccentDim
import com.example.kopilka.ui.theme.ColorBase
import com.example.kopilka.ui.theme.ColorBorderAccent
import com.example.kopilka.ui.theme.ColorError
import com.example.kopilka.ui.theme.ColorSurface
import com.example.kopilka.ui.theme.ColorText
import com.example.kopilka.ui.theme.ColorText3
import com.example.kopilka.viewmodel.PinPhase
import com.example.kopilka.viewmodel.PinUiState
import com.example.kopilka.viewmodel.PinViewModel
import com.example.kopilka.utils.BiometricAuthResult
import com.example.kopilka.utils.BiometricAuthenticator
import kotlinx.coroutines.launch


@Composable
fun PinScreen(
    onUnlocked: () -> Unit,
    viewModel: PinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current as? FragmentActivity

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) onUnlocked()
    }

    PinScreenContent(
        state = state,
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onShakeConsumed = viewModel::consumeShake,
        onBiometricClick = {
            activity?.let { act ->
                BiometricAuthenticator.authenticate(
                    activity = act,
                    onResult = { result ->
                        when (result) {
                            is BiometricAuthResult.Success -> viewModel.onBiometricSuccess()
                            is BiometricAuthResult.Error -> viewModel.onBiometricError(result.message)
                            BiometricAuthResult.Cancelled -> Unit // пользователь сам выбрал PIN — ничего не делаем
                        }
                    }
                )
            }
        }

        //onForgotPin = viewModel::onForgotPin
    )
}

@Composable
private fun PinScreenContent(
    state: PinUiState,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onShakeConsumed: () -> Unit,
    onBiometricClick: () -> Unit

    //onForgotPin: () -> Unit
) {
    if (state.phase == PinPhase.LOADING) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorBase)
        )
        return
    }

    val (title, subtitle) = when (state.phase) {
        PinPhase.CREATE -> "Создать PIN" to "Придумайте 4-значный код для защиты накоплений"
        PinPhase.REENTER -> "Подтвердите PIN" to "Введите PIN ещё раз для подтверждения"
        PinPhase.UNLOCK -> "С возвращением" to "Введите PIN, чтобы разблокировать Копилку"
        PinPhase.LOADING -> "" to ""
    }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.shake) {
        if (state.shake) {
            scope.launch {
                val keyframes = listOf(0f, -8f, 8f, -8f, 8f, 0f)
                for (x in keyframes) {
                    shakeOffset.animateTo(x, animationSpec = tween(60))
                }
                onShakeConsumed()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBase)
            .padding(horizontal = 28.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Логотип + заголовок
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ColorAccentDim, RoundedCornerShape(28.dp))
                    .border(1.dp, ColorBorderAccent, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Иконка"
                )
            }
            Column(
                modifier = Modifier.padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "КОПИЛКА",
                    color = ColorAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = title,
                    color = ColorText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = subtitle,
                    color = ColorText3,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .widthIn(max = 220.dp)
                )
            }
        }

        // Точки PIN + текст ошибки
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val filled = i < state.pin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (filled) ColorAccent else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    2.dp,
                                    if (filled) ColorAccent else Color.White.copy(alpha = 0.18f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
            Text(
                text = state.error ?: " ",
                color = ColorError,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        // Клавиатура 3x4
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "BIO", "0", "⌫")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.widthIn(max = 264.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false
        ) {
            items(keys) { key ->
                if (key == "BIO") {
                    val showBiometric = state.biometricAvailable &&
                            state.phase == PinPhase.UNLOCK &&
                            !state.isLockedOut
                    if (showBiometric) {
                        BiometricKey(onClick = onBiometricClick)
                    } else {
                        // Сохраняем размер ячейки, чтобы сетка не "прыгала"
                        Box(modifier = Modifier.aspectRatio(1.15f))
                    }
                } else {
                    PinKey(
                        label = key,
                        enabled = !state.isLockedOut,
                        onClick = {
                            when (key) {
                                "" -> Unit
                                "⌫" -> onBackspace()
                                else -> onDigit(key)
                            }
                        }
                    )
                }

            }

        // Ссылка "Забыли PIN?" — только на экране разблокировки
//        Box(modifier = Modifier.height(20.dp)) {
//            if (state.phase == PinPhase.UNLOCK) {
//                Text(
//                    text = "Забыли PIN? Сбросить",
//                    color = ColorAccent,
//                    fontSize = 13.sp,
//                    modifier = Modifier.clickable { onForgotPin() }.clip(RoundedCornerShape(20.dp))
//                )
//            }
//        }
        }
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    val isBackspace = label == "⌫"
    val isEmpty = label.isEmpty()

    Box(
        modifier = Modifier
            .aspectRatio(1.15f)
            .then(
                if (!isEmpty && !isBackspace)
                    Modifier
                        .background(ColorSurface, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                else if (isBackspace)
                    Modifier.clip(RoundedCornerShape(20.dp))
                else Modifier
            )
            .then(
                if (!isEmpty && enabled) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            Text(
                text = label,
                color = if (isBackspace) ColorText3 else ColorText,
                fontSize = if (isBackspace) 20.sp else 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Кнопка "Войти по отпечатку" — визуально оформлена как обычная клавиша клавиатуры. */
@Composable
private fun BiometricKey(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1.15f)
            .background(ColorSurface, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, ColorBorderAccent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.fngerprint),
            contentDescription = "Моя иконка",
            modifier = Modifier.size(28.dp),
            colorFilter = ColorFilter.tint(ColorAccent)
        )
    }
}

