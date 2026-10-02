package com.jose.calendario

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class Tema(
    val id: String,
    val nombre: String,
    val scheme: ColorScheme,
    val festivo: Color,
    val muestra: List<Color>,
)

val TEMAS = listOf(
    Tema(
        id = "claro", nombre = "Claro",
        scheme = lightColorScheme(
            primary = Color(0xFF4F6DF5), onPrimary = Color(0xFFFFFFFF),
            background = Color(0xFFEEF1F7), onBackground = Color(0xFF1C2333),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1C2333),
            surfaceVariant = Color(0xFFF4F6FB), onSurfaceVariant = Color(0xFF4A5570),
            outline = Color(0xFF98A1B8), outlineVariant = Color(0xFFE2E7F0),
            secondary = Color(0xFF5B6CFF), error = Color(0xFFE5484D),
        ),
        festivo = Color(0xFFD92626),
        muestra = listOf(Color(0xFFEEF1F7), Color(0xFFFFFFFF), Color(0xFF4F6DF5), Color(0xFFD92626)),
    ),
    Tema(
        id = "oscuro", nombre = "Oscuro",
        scheme = darkColorScheme(
            primary = Color(0xFF7B96FF), onPrimary = Color(0xFF0C1020),
            background = Color(0xFF0E1117), onBackground = Color(0xFFE7ECF5),
            surface = Color(0xFF171C26), onSurface = Color(0xFFE7ECF5),
            surfaceVariant = Color(0xFF1F2634), onSurfaceVariant = Color(0xFFB7C0D4),
            outline = Color(0xFF71809C), outlineVariant = Color(0xFF262E40),
            secondary = Color(0xFF9AB2FF), error = Color(0xFFFF5C5C),
        ),
        festivo = Color(0xFFFF6B6B),
        muestra = listOf(Color(0xFF0E1117), Color(0xFF171C26), Color(0xFF7B96FF), Color(0xFFFF6B6B)),
    ),
    Tema(
        id = "noche", nombre = "Azul noche",
        scheme = darkColorScheme(
            primary = Color(0xFF6F9BFF), onPrimary = Color(0xFF08122E),
            background = Color(0xFF0A0F2B), onBackground = Color(0xFFE6EBFF),
            surface = Color(0xFF131A3D), onSurface = Color(0xFFE6EBFF),
            surfaceVariant = Color(0xFF1A2350), onSurfaceVariant = Color(0xFFAAB6E8),
            outline = Color(0xFF7383C4), outlineVariant = Color(0xFF273263),
            secondary = Color(0xFF8FB0FF), error = Color(0xFFFF5D5D),
        ),
        festivo = Color(0xFFFF7575),
        muestra = listOf(Color(0xFF0A0F2B), Color(0xFF131A3D), Color(0xFF6F9BFF), Color(0xFFFF7575)),
    ),
    Tema(
        id = "bosque", nombre = "Bosque",
        scheme = darkColorScheme(
            primary = Color(0xFF4CC38A), onPrimary = Color(0xFF06130B),
            background = Color(0xFF0E1712), onBackground = Color(0xFFE8F3EA),
            surface = Color(0xFF152219), onSurface = Color(0xFFE8F3EA),
            surfaceVariant = Color(0xFF1D2F22), onSurfaceVariant = Color(0xFFBCD4C2),
            outline = Color(0xFF7D9A86), outlineVariant = Color(0xFF26402F),
            secondary = Color(0xFF7FD6A8), error = Color(0xFFFF6363),
        ),
        festivo = Color(0xFFFF8080),
        muestra = listOf(Color(0xFF0E1712), Color(0xFF152219), Color(0xFF4CC38A), Color(0xFFFF8080)),
    ),
    Tema(
        id = "pastel", nombre = "Pastel",
        scheme = lightColorScheme(
            primary = Color(0xFFC66BD6), onPrimary = Color(0xFFFFFFFF),
            background = Color(0xFFFDF3F7), onBackground = Color(0xFF43304A),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF43304A),
            surfaceVariant = Color(0xFFFAF0F6), onSurfaceVariant = Color(0xFF6F5A78),
            outline = Color(0xFFB0A0B8), outlineVariant = Color(0xFFF2DFEA),
            secondary = Color(0xFFD98FE6), error = Color(0xFFE5484D),
        ),
        festivo = Color(0xFFE05263),
        muestra = listOf(Color(0xFFFDF3F7), Color(0xFFFFFFFF), Color(0xFFC66BD6), Color(0xFFE05263)),
    ),
    Tema(
        id = "cafe", nombre = "Café",
        scheme = lightColorScheme(
            primary = Color(0xFFB07D3F), onPrimary = Color(0xFFFFF8EE),
            background = Color(0xFFF4EDE2), onBackground = Color(0xFF3D3227),
            surface = Color(0xFFFFFDF8), onSurface = Color(0xFF3D3227),
            surfaceVariant = Color(0xFFF5EEE1), onSurfaceVariant = Color(0xFF645646),
            outline = Color(0xFFA5977F), outlineVariant = Color(0xFFE6DBC8),
            secondary = Color(0xFFC99B62), error = Color(0xFFD64545),
        ),
        festivo = Color(0xFFC9342C),
        muestra = listOf(Color(0xFFF4EDE2), Color(0xFFFFFDF8), Color(0xFFB07D3F), Color(0xFFC9342C)),
    ),
)

fun temaDe(id: String): Tema = TEMAS.firstOrNull { it.id == id } ?: TEMAS.first()
