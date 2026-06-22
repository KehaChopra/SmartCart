package com.yourbusiness.smartkart.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.ui.auth.model.Country
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodeSelector(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inline: Boolean = false
) {
    if (inline) {
        InlineCountryCodeSelector(
            selectedCountry = selectedCountry,
            onCountrySelected = onCountrySelected,
            modifier = modifier,
            enabled = enabled
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selectedCountry.flagEmoji}  ${selectedCountry.dialCode}",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .menuAnchor()
                .width(130.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Country.countries.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = country.flagEmoji,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = country.dialCode,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = country.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onCountrySelected(country)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineCountryCodeSelector(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .menuAnchor()
                .clickable(enabled = enabled) { if (enabled) expanded = true }
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedCountry.isoCode}  ${selectedCountry.dialCode}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Country.countries.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Text("${country.isoCode}  ${country.dialCode}  ${country.name}")
                    },
                    onClick = {
                        onCountrySelected(country)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SmartKartPhoneInputField(
    selectedCountry: Country,
    phoneNumber: String,
    phoneError: String?,
    enabled: Boolean,
    onCountrySelected: (Country) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val targetBorderColor = when {
        phoneError != null -> MaterialTheme.colorScheme.error
        isFocused -> SmartKartGreen
        else -> MaterialTheme.colorScheme.outline
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        label = "phone_border_color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused && phoneError == null) 1.5.dp else 1.dp,
        label = "phone_border_width"
    )

    Column(modifier = modifier) {
        Text(
            text = "Phone Number",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(width = borderWidth, color = borderColor, shape = shape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountryCodeSelector(
                    selectedCountry = selectedCountry,
                    onCountrySelected = onCountrySelected,
                    enabled = enabled,
                    inline = true
                )

                VerticalDivider(
                    modifier = Modifier.height(28.dp),
                    color = MaterialTheme.colorScheme.outline
                )

                BasicTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        Box {
                            if (phoneNumber.isEmpty()) {
                                Text(
                                    text = "Enter mobile number",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        if (phoneError != null) {
            Text(
                text = phoneError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
