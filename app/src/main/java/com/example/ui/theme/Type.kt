package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val FrauncesFontFamily = FontFamily(
  Font(R.font.fraunces, FontWeight.Normal),
  Font(R.font.fraunces, FontWeight.SemiBold),
  Font(R.font.fraunces, FontWeight.Bold),
)

val PlusJakartaFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val HearthTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
      ),
  )
