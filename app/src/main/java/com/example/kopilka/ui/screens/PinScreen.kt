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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.example.kopilka.ui.theme.ColorBorder
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
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
        onPhraseChange = viewModel::onPhraseChange,
        onPhraseSubmit = viewModel::onPhraseSubmit,
        onForgotPin = viewModel::onForgotPin,
        onBackToUnlock = viewModel::onBackToUnlock,
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

    )
}

@Composable
private fun PinScreenContent(
    state: PinUiState,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onShakeConsumed: () -> Unit,
    onBiometricClick: () -> Unit,
    onPhraseChange: (String) -> Unit,
    onBackToUnlock: () -> Unit,
    onPhraseSubmit: () -> Unit,
    onForgotPin: () -> Unit,




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
        PinPhase.PHRASE_CREATE -> "Контрольная фраза" to "Она понадобится, чтобы восстановить доступ, если вы забудете PIN"
        PinPhase.UNLOCK -> "С возвращением" to "Введите PIN"
        PinPhase.RECOVER -> "Восстановление" to "Введите контрольную фразу, чтобы задать новый PIN"
        PinPhase.LOADING -> "" to ""
    }

    val isPhrasePhase = state.phase == PinPhase.PHRASE_CREATE || state.phase == PinPhase.RECOVER

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

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
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .background(ColorBase)
            .padding(horizontal = 28.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Логотип + заголовок
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if(!isPhrasePhase)
            {
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
            }

            Column(
                modifier = Modifier.padding(top = if (isPhrasePhase) 0.dp else 20.dp),
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
                        .padding(top = 8.dp, bottom = 10.dp)
                        .widthIn(max = 260.dp)
                )
            }
        }

        if (isPhrasePhase) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ){
                Spacer(Modifier.height(24.dp))
                PhraseSection(
                    state = state,
                    shakeX = shakeOffset.value,
                    onPhraseChange = onPhraseChange,
                    onSubmit = onPhraseSubmit,
                    onBack = onBackToUnlock
                )
            }
        } else {
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
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }

            // Клавиатура 3x4
            Column(horizontalAlignment = Alignment.CenterHorizontally)
            {
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
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.height(28.dp)) {
                    if (state.phase == PinPhase.UNLOCK) {
                        Text(
                            text = "Забыли PIN?",
                            color = ColorAccent,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onForgotPin() }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
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

@Composable
private fun PhraseSection(
    state: PinUiState,
    shakeX: Float,
    onPhraseChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val isRecover = state.phase == PinPhase.RECOVER
    val canSubmit = state.phrase.isNotBlank() && !state.isLockedOut

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer { translationX = shakeX }
                .fillMaxWidth()
                .background(ColorSurface, RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (state.error != null) ColorError else ColorBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            BasicTextField(
                value = state.phrase,
                onValueChange = onPhraseChange,
                singleLine = true,
                enabled = !state.isLockedOut,
                textStyle = TextStyle(color = ColorText, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(ColorAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (state.phrase.isEmpty()) {
                        Text("Например, кличка первого питомца", color = ColorText3, fontSize = 15.sp)
                    }
                    inner()
                }
            )
        }

        Text(
            text = state.error ?: " ",
            color = ColorError,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (canSubmit) ColorAccent else ColorSurface)
                .clickable(enabled = canSubmit, onClick = onSubmit)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRecover) "Сбросить PIN" else "Продолжить",
                color = if (canSubmit) ColorBase else ColorText3,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isRecover) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Назад",
                color = ColorText3,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

