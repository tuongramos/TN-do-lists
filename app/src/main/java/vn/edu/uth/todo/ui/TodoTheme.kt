package vn.edu.uth.todo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import vn.edu.uth.todo.R

object TodoDesign {
    val Blue = Color(0xFF2F5BEA)
    val Ink = Color(0xFF1C1C1E)
    val Body = Color(0xFF5C5C62)
    val Muted = Color(0xFF6B6B70)
    val Inactive = Color(0xFFD1D1D6)

    val Font = FontFamily(
        Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
        Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
        Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
        Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
        Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold)
    )
}

@Composable
fun TodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TodoDesign.Blue,
            onPrimary = Color.White,
            background = Color.White,
            onBackground = TodoDesign.Ink
        ),
        content = content
    )
}
