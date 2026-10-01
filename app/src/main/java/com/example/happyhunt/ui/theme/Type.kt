package com.example.happyhunt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.happyhunt.R

/*
 * Nunito throughout (SIL Open Font License, see app/licenses): rounded,
 * warm and very readable at small sizes. It is a variable font; each weight
 * used here is a named instance of the one bundled file.
 */

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Nunito = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800), nunito(900))

private val tight = (-0.01).em

val HuntTypography = Typography(
    displayLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.02).em),
    displayMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em),
    displaySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em),
    headlineLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = tight),
    headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = tight),
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = tight),
    titleLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = tight),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
)
