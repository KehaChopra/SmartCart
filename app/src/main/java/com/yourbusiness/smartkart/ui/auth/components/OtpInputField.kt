package com.yourbusiness.smartkart.ui.auth.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpInputField(
    otpDigits: List<String>,
    onDigitChange: (index: Int, digit: String) -> Unit,
    onBackspace: (index: Int) -> Unit,
    isError: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = true
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }
    val focusedBorderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        otpDigits.forEachIndexed { index, digit ->
            OtpDigitBox(
                value = digit,
                enabled = enabled,
                borderColor = if (digit.isNotEmpty()) focusedBorderColor else borderColor,
                focusRequester = focusRequesters[index],
                onValueChange = { newValue ->
                    val sanitized = newValue.filter { it.isDigit() }
                    when {
                        sanitized.length > 1 -> {
                            val lastDigit = sanitized.last().toString()
                            onDigitChange(index, lastDigit)
                            if (index < 5) focusRequesters[index + 1].requestFocus()
                        }
                        sanitized.length == 1 -> {
                            onDigitChange(index, sanitized)
                            if (index < 5) focusRequesters[index + 1].requestFocus()
                        }
                        sanitized.isEmpty() -> onBackspace(index)
                    }
                },
                onBackspaceAtEmpty = {
                    if (index > 0) {
                        onBackspace(index - 1)
                        focusRequesters[index - 1].requestFocus()
                    }
                }
            )
        }
    }

    if (requestInitialFocus) {
        LaunchedEffect(enabled, otpDigits.joinToString()) {
            if (enabled && otpDigits.all { it.isEmpty() }) {
                focusRequesters[0].requestFocus()
            }
        }
    }
}

@Composable
private fun OtpDigitBox(
    value: String,
    enabled: Boolean,
    borderColor: androidx.compose.ui.graphics.Color,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspaceAtEmpty: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyUp &&
                        event.key == Key.Backspace &&
                        value.isEmpty()
                    ) {
                        onBackspaceAtEmpty()
                        true
                    } else {
                        false
                    }
                },
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    innerTextField()
                }
            }
        )
    }
}
