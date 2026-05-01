package com.ugurbuga.blockgames.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.nunito_black
import blockgames.composeapp.generated.resources.nunito_bold
import blockgames.composeapp.generated.resources.nunito_extrabold
import blockgames.composeapp.generated.resources.nunito_medium
import blockgames.composeapp.generated.resources.nunito_regular
import blockgames.composeapp.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun blockGamesTypography(): Typography {
    val nunito = FontFamily(
        Font(resource = Res.font.nunito_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.nunito_medium, weight = FontWeight.Medium),
        Font(resource = Res.font.nunito_semibold, weight = FontWeight.SemiBold),
        Font(resource = Res.font.nunito_bold, weight = FontWeight.Bold),
        Font(resource = Res.font.nunito_extrabold, weight = FontWeight.ExtraBold),
        Font(resource = Res.font.nunito_black, weight = FontWeight.Black),
    )

    return remember(nunito) {
        Typography(
            displayLarge = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Black, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
            displayMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
            displaySmall = TextStyle(fontFamily = nunito, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp),
            headlineLarge = TextStyle(fontFamily = nunito, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
            headlineMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
            headlineSmall = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
            titleLarge = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
            titleSmall = TextStyle(fontFamily = nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
            bodyLarge = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
            bodyMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.15.sp),
            bodySmall = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
            labelLarge = TextStyle(fontFamily = nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
            labelMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
            labelSmall = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        )
    }
}

