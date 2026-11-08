package dev.andre.nutritionist.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Набор иконок приложения
 */
object FitIcons {

    val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitMenu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 18f)
                lineTo(20f, 18f)
            }
        }.build()
    }

    val Document: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitDocument",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 3f)
                lineTo(18f, 3f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 5f)
                lineTo(20f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 21f)
                lineTo(6f, 21f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 19f)
                lineTo(4f, 5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6f, 3f)
                close()
                moveTo(8f, 8f)
                lineTo(16f, 8f)
                moveTo(8f, 12f)
                lineTo(16f, 12f)
                moveTo(8f, 16f)
                lineTo(12f, 16f)
            }
        }.build()
    }

    val Meals: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitMeals",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Нож
                moveTo(4f, 20f)
                lineTo(10f, 14f)
                moveTo(10f, 14f)
                lineTo(18f, 6f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15f, 3f)
                lineTo(7f, 11f)
                lineTo(10f, 14f)

                // Вилка
                moveTo(20f, 20f)
                lineTo(14f, 14f)
                moveTo(14f, 14f)
                lineTo(8f, 8f)
                moveTo(16f, 4f)
                lineTo(20f, 8f)
            }
        }.build()
    }

    val Statistics: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitStatistics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Круг
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 21f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3f)
                close()
                // Оси прицела
                moveTo(12f, 2f)
                lineTo(12f, 22f)
                moveTo(2f, 12f)
                lineTo(22f, 12f)
            }
        }.build()
    }

    val Profile: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitProfile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Голова
                moveTo(12f, 7f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 15f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 7f)
                close()
                // Плечи
                moveTo(5f, 21f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 21f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 16f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 8f)
                close()
                moveTo(12f, 2f)
                lineTo(12f, 4f)
                moveTo(12f, 20f)
                lineTo(12f, 22f)
                moveTo(2f, 12f)
                lineTo(4f, 12f)
                moveTo(20f, 12f)
                lineTo(22f, 12f)
                moveTo(4.93f, 4.93f)
                lineTo(6.34f, 6.34f)
                moveTo(17.66f, 17.66f)
                lineTo(19.07f, 19.07f)
                moveTo(4.93f, 19.07f)
                lineTo(6.34f, 17.66f)
                moveTo(17.66f, 6.34f)
                lineTo(19.07f, 4.93f)
            }
        }.build()
    }

    val Notifications: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitNotifications",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 8f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                lineTo(21f, 17f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                close()
                moveTo(10.3f, 21f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 13.7f, 21f)
            }
        }.build()
    }

    val Help: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitHelp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 19.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.5f, 17f)
                lineTo(20f, 17f)
                moveTo(4f, 19.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.5f, 22f)
                lineTo(20f, 22f)
                moveTo(4f, 19.5f)
                lineTo(4f, 4.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.5f, 2f)
                lineTo(20f, 2f)
                lineTo(20f, 22f)
            }
        }.build()
    }

    /** Иконка вилки и ножа вертикальная (для карточки приема пищи). */
    val MealForkKnife: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitMealForkKnife",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Вилка
                moveTo(7f, 3f)
                lineTo(7f, 9f)
                moveTo(5f, 3f)
                lineTo(5f, 7f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9f, 7f)
                lineTo(9f, 3f)
                moveTo(7f, 9f)
                lineTo(7f, 21f)

                // Нож
                moveTo(17f, 3f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15f, 5f)
                lineTo(15f, 12f)
                lineTo(17f, 12f)
                lineTo(17f, 21f)
            }
        }.build()
    }

    /** Иконка почты (конверт) */
    val Mail: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitMail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 6f)
                lineTo(22f, 18f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 20f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 18f)
                lineTo(2f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 4f)
                close()
                moveTo(2.5f, 6.5f)
                lineTo(12f, 13f)
                lineTo(21.5f, 6.5f)
            }
        }.build()
    }

    /** Иконка ключа (пароль) */
    val Key: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitKey",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.5f, 8f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 7.5f, 17f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 7.5f, 8f)
                close()
                moveTo(12f, 12.5f)
                lineTo(21f, 12.5f)
                moveTo(17f, 12.5f)
                lineTo(17f, 15.5f)
                moveTo(20f, 12.5f)
                lineTo(20f, 15.5f)
            }
        }.build()
    }

    /** Иконка Telegram */
    val Telegram: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitTelegram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color(0xFF2AABEE))) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 22f)
                arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(6.5f, 11.5f)
                lineTo(17f, 7f)
                lineTo(14.5f, 17.5f)
                lineTo(10.5f, 13.5f)
                lineTo(6.5f, 11.5f)
                close()
            }
        }.build()
    }

    /** Иконка Google */
    val Google: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitGoogle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color(0xFFEA4335)),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20f, 12f)
                lineTo(12f, 12f)
                moveTo(19f, 8f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5f, 12f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18.5f, 15.5f)
            }
        }.build()
    }
}
