package com.danilkinkin.buckwheat.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private fun getAnnotatedString(
    value: String,
    hintParts: List<Pair<Int, Int>>,
    styles: List<SpanStyle>,
): AnnotatedString {
    val builder = AnnotatedString.Builder(value)
    hintParts.forEachIndexed { index, part ->
        builder.addStyle(styles[index], part.first, part.second)
    }
    return builder.toAnnotatedString()
}

private fun getAnnotatedString(
    value: String,
    hintParts: List<Pair<Int, Int>>,
    hintColor: Color,
): AnnotatedString {
    return getAnnotatedString(
        value,
        hintParts,
        hintParts.map { SpanStyle(color = hintColor) }
    )
}

private fun getAnnotatedString(
    value: String,
    hintPart: Pair<Int, Int>,
    hintColor: Color,
): AnnotatedString {
    return getAnnotatedString(value, listOf(hintPart), hintColor)
}

private fun calcShift(before: String, after: String, position: Int): Int {
    var shift = 0

    for (i in 0 until position) {
        while (i < before.length && i + shift < after.length && (before[i] != after[i + shift])) {
            shift += 1
        }
    }

    return shift
}

private fun calcMinShift(before: String, after: String): Int {
    var shift = 0

    if (before.isEmpty() || after.isEmpty()) return 0

    while (shift < after.length && (before.first() != after[shift])) {
        shift += 1
    }

    return shift
}

private fun visualTransformationAsCurrency(
    input: AnnotatedString,
    currency: ExtendCurrency,
    hintColor: Color,
    placeholder: String = "",
    placeholderStyle: SpanStyle = SpanStyle(),
): TransformedText {
    val floatDivider = getFloatDivider()
    val fixed = tryConvertStringToNumber(input.text)
    val currSymbol = prettyCandyCanes(
        0.toBigDecimal(),
        currency,
        maximumFractionDigits = 0,
        minimumFractionDigits = 0,
    ).filter { it !='0' }
    val output = prettyCandyCanes(
        input.text.ifEmpty { "0" }.toBigDecimal(),
        currency,
        maximumFractionDigits = 2,
        minimumFractionDigits = 1,
    ).replace(currSymbol, "").trim()

    val offsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val shift = calcShift(input.text.replace(".", floatDivider), output, offset)
            val minShift = calcMinShift(input.text.replace(".", floatDivider), output)

            return (offset + shift).coerceIn(max(minShift, 0), output.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
            val shift = calcShift(input.text.replace(".", floatDivider), output, offset)

            return (offset - shift).coerceIn(min(0, input.length), input.length)
        }
    }

    val forceShowAfterDot = input.text.contains(".0")
    val before = output.substringBefore("${floatDivider}0")
    val after = if (forceShowAfterDot) {
        output.substringAfter(floatDivider, "")
    } else {
        output.substringAfter("${floatDivider}0", "")
    }

    val divider = if (fixed.third.isNotEmpty() || forceShowAfterDot) {
        "$floatDivider${fixed.third}"
    } else {
        ""
    }

    val heightFixer = if (currSymbol.isEmpty()) " " else ""

    return if (input.text.isEmpty()) {
        TransformedText(
            getAnnotatedString(
                placeholder + heightFixer,
                listOf(
                    Pair(
                        0,
                        placeholder.length,
                    ),
                ),
                listOf(
                    placeholderStyle.copy(
                        color = hintColor,
                    ),
                    SpanStyle(color = hintColor),
                ),
            ),
            offsetTranslator,
        )
    } else {
        TransformedText(
            getAnnotatedString(
                before + divider + after,
                listOf(
                    Pair(
                        before.length + (if (fixed.third.isNotEmpty()) 1 else 0),
                        before.length + (if (fixed.third.isNotEmpty()) 2 else 0),
                    ),
                ),
                listOf(
                    SpanStyle(color = hintColor),
                ),
            ),
            offsetTranslator,
        )
    }
}

fun visualTransformationAsCurrency(
    currency: ExtendCurrency,
    hintColor: Color,
    placeholder: String = "",
    placeholderStyle: SpanStyle = SpanStyle(),
): ((input: AnnotatedString) -> TransformedText) {
    return {
        visualTransformationAsCurrency(it, currency, hintColor, placeholder, placeholderStyle)
    }
}

fun isNumber(char: Char): Boolean {
    return try {
        char.toString().toInt(); true
    } catch (e: Exception) {
        false
    }
}

fun Triple<String, String, String>.join(third: Boolean = true): String = this.first + this.second + if (third) this.third else ""

fun fixedNumberString(input: String): String {
    val floatDivider = getFloatDivider()

    val before = input.substringBefore(floatDivider)
    val after = input.substringAfter(floatDivider, "")

    val beforeFiltered = before.replace("\\D".toRegex(), "")
    val afterFiltered = after.replace("\\D".toRegex(), "")

    if (beforeFiltered.isEmpty() && afterFiltered.isEmpty()) return ""

    return "$beforeFiltered.$afterFiltered"
}

fun tryConvertStringToNumber(input: String): Triple<String, String, String> {
    val afterDot = input.dropWhile { it != '.' }
    val beforeDot = input.substring(0, input.length - afterDot.length)

    val start = beforeDot.filter { isNumber(it) }.dropWhile { it == '0' }
    val hintStart = if (start.isEmpty()) "0" else ""
    val end = afterDot.filter { isNumber(it) }
    var hintEnd = ""
    if (end.isEmpty() && input.lastOrNull() == '.') {
        hintEnd = "0"
    }
    val middle = if (end.isNotEmpty() || (input.lastOrNull() == '.')) {
        "."
    } else {
        ""
    }

    return Triple(
        hintStart,
        "$start$middle${end.substring(0, min(2, end.length))}",
        hintEnd,
    )
}

@Preview
@Composable
fun Preview() {
    Column {
        Text(
            text = visualTransformationAsCurrency(
                getAnnotatedString("0", Pair(0, 1), Color.Green),
                currency = ExtendCurrency.none(),
                Color.Green,
            ).text
        )
        Text(
            text = visualTransformationAsCurrency(
                getAnnotatedString("0", Pair(0, 4), Color.Green),
                currency = ExtendCurrency.getInstance("EUR"),
                Color.Green,
            ).text
        )
        Text(
            text = visualTransformationAsCurrency(
                getAnnotatedString("0", Pair(0, 4), Color.Green),
                currency = ExtendCurrency.getInstance("RUB"),
                Color.Green,
            ).text
        )
        Text(
            text = visualTransformationAsCurrency(
                getAnnotatedString("", Pair(0, 4), Color.Green),
                currency = ExtendCurrency.none(),
                Color.Green,
                placeholder = "PLCHDR",
                placeholderStyle = SpanStyle(
                    fontSize = 6.sp,
                    fontWeight = FontWeight.W700,
                    baselineShift = BaselineShift(0.25f)
                ),
            ).text
        )
        Text(
            text = visualTransformationAsCurrency(
                getAnnotatedString("", Pair(0, 4), Color.Green),
                currency = ExtendCurrency.getInstance("EUR"),
                Color.Green,
                placeholder = "PLCHDR",
                placeholderStyle = SpanStyle(
                    fontSize = 6.sp,
                    fontWeight = FontWeight.W700,
                    baselineShift = BaselineShift(0.25f)
                ),
            ).text
        )
        Text(
            text = visualTransformationAsCurrency(
                getAnnotatedString("", Pair(0, 4), Color.Green),
                currency = ExtendCurrency.getInstance("RUB"),
                Color.Green,
                placeholder = "PLCHDR"
            ).text
        )
    }
}