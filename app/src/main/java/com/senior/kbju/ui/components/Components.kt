package com.senior.kbju.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senior.kbju.ui.theme.CardBorder
import com.senior.kbju.ui.theme.ErrorRed
import com.senior.kbju.ui.theme.SurfaceWhite
import com.senior.kbju.ui.theme.TextDark
import com.senior.kbju.ui.theme.TextMuted
import com.senior.kbju.utils.sanitizeNumberInput

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceWhite,
    borderColor: Color = CardBorder,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OutlineBigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    buttonHeight: Dp = 56.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = buttonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = TextDark,
            disabledContainerColor = Color(0xFFF1F5F9),
            disabledContentColor = Color(0xFF94A3B8)
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BigToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 62.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = if (selected) null else BorderStroke(1.5.dp, CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            contentColor = if (selected) Color.White else TextDark,
            disabledContainerColor = Color(0xFFE2E8F0),
            disabledContentColor = Color(0xFF94A3B8)
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark
    )
}

@Composable
fun ErrorText(text: String?, modifier: Modifier = Modifier) {
    if (text != null) {
        Text(
            text = text,
            modifier = modifier.fillMaxWidth(),
            color = ErrorRed,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Поле для чисел: цифровая клавиатура, ввод очищается от мусора, ошибки не роняют приложение.
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            if (suffix != null) {
                Text(text = suffix, fontSize = 19.sp, color = TextMuted)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeNumberInput(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = CardBorder
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

// Поле для обычного текста (название продукта, поиск).
@Composable
fun PlainTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = CardBorder
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 21.sp, color = TextDark)
        )
    }
}