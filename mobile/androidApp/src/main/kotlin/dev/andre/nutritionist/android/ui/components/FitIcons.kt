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

    /** Иконка выхода из аккаунта (Logout) */
    val Logout: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitLogout",
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
                // Дверь / контур
                moveTo(9f, 21f)
                lineTo(5f, 21f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 19f)
                lineTo(3f, 5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 3f)
                lineTo(9f, 3f)
                // Стрелка наружу
                moveTo(16f, 17f)
                lineTo(21f, 12f)
                lineTo(16f, 7f)
                moveTo(21f, 12f)
                lineTo(9f, 12f)
            }
        }.build()
    }

    /** Иконка карандаша / редактирования (Edit) */
    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitEdit",
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
                moveTo(3f, 17.25f)
                lineTo(3f, 21f)
                lineTo(6.75f, 21f)
                lineTo(17.81f, 9.94f)
                lineTo(14.06f, 6.19f)
                close()
                moveTo(20.71f, 7.04f)
                lineTo(16.96f, 3.29f)
            }
        }.build()
    }

    /** Иконка утреннего солнца (Завтрак) */
    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitSun",
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
                // Круг солнца
                moveTo(12f, 7f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 17f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 7f)
                close()
                // Лучи
                moveTo(12f, 1f); lineTo(12f, 4f)
                moveTo(12f, 20f); lineTo(12f, 23f)
                moveTo(4.22f, 4.22f); lineTo(6.34f, 6.34f)
                moveTo(17.66f, 17.66f); lineTo(19.78f, 19.78f)
                moveTo(1f, 12f); lineTo(4f, 12f)
                moveTo(20f, 12f); lineTo(23f, 12f)
                moveTo(4.22f, 19.78f); lineTo(6.34f, 17.66f)
                moveTo(17.66f, 6.34f); lineTo(19.78f, 4.22f)
            }
        }.build()
    }

    /** Иконка полуденного солнца (Обед) */
    val Noon: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitNoon",
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
                moveTo(12f, 2f); lineTo(12f, 5f)
                moveTo(12f, 19f); lineTo(12f, 22f)
                moveTo(2f, 12f); lineTo(5f, 12f)
                moveTo(19f, 12f); lineTo(22f, 12f)
                moveTo(4.93f, 4.93f); lineTo(7.05f, 7.05f)
                moveTo(16.95f, 16.95f); lineTo(19.07f, 19.07f)
                moveTo(4.93f, 19.07f); lineTo(7.05f, 16.95f)
                moveTo(16.95f, 7.05f); lineTo(19.07f, 4.93f)
            }
        }.build()
    }

    /** Иконка заката (Ужин) */
    val Sunset: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitSunset",
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
                // Горизонт
                moveTo(2f, 18f); lineTo(22f, 18f)
                moveTo(4f, 21f); lineTo(20f, 21f)
                // Заходящее солнце
                moveTo(7f, 18f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 18f)
                // Лучи заката
                moveTo(12f, 6f); lineTo(12f, 9f)
                moveTo(5.64f, 10.64f); lineTo(7.76f, 12.76f)
                moveTo(18.36f, 10.64f); lineTo(16.24f, 12.76f)
            }
        }.build()
    }

    /** Иконка полумесяца (Перекус / Другое) */
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitMoon",
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
                moveTo(21f, 12.79f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.21f, 3f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 21f, 12.79f)
                close()
            }
        }.build()
    }

    /** Иконка плюса (+) */
    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }.build()
    }

    /** Иконка пламени (серия / калории) */
    val Flame: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitFlame",
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
                moveTo(8.5f, 14.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 18f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15.5f, 14.5f)
                curveTo(15.5f, 11.5f, 12f, 9f, 12f, 9f)
                curveTo(12f, 9f, 8.5f, 11.5f, 8.5f, 14.5f)
                close()
                moveTo(12f, 2f)
                curveTo(12f, 2f, 6f, 7f, 6f, 14f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 14f)
                curveTo(18f, 7f, 12f, 2f, 12f, 2f)
                close()
            }
        }.build()
    }

    /** Иконка галочки (Check) */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 12f)
                lineTo(9.5f, 17f)
                lineTo(20f, 6.5f)
            }
        }.build()
    }

    /** Шеврон вниз (раскрыть) */
    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitChevronDown",
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
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }.build()
    }

    /** Шеврон вверх (свернуть) */
    val ChevronUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitChevronUp",
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
                moveTo(6f, 15f)
                lineTo(12f, 9f)
                lineTo(18f, 15f)
            }
        }.build()
    }

    /** Шеврон вправо */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitChevronRight",
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
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }.build()
    }

    /** Иконка миски / йогурта / еды */
    val Bowl: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitBowl",
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
                // Миска
                moveTo(3f, 11f)
                lineTo(21f, 11f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 20f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 11f)
                close()
                // Ложка в миске
                moveTo(16f, 5f)
                lineTo(11f, 11f)
            }
        }.build()
    }

    /** Иконка упражнения / активности (человек + плюс) */
    val Exercise: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitExercise",
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
                moveTo(14f, 4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 8f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 4f)
                close()
                // Тело и ноги
                moveTo(10f, 9f); lineTo(14f, 12f); lineTo(12f, 17f); lineTo(15f, 21f)
                moveTo(14f, 12f); lineTo(18f, 14f)
                moveTo(12f, 17f); lineTo(9f, 20f)
                // Плюс слева
                moveTo(4f, 7f); lineTo(4f, 13f)
                moveTo(1f, 10f); lineTo(7f, 10f)
            }
        }.build()
    }

    /** Иконка информации (i) */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitInfo",
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
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 22f)
                arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
                close()
                moveTo(12f, 16f); lineTo(12f, 11f)
                moveTo(12f, 8f); lineTo(12.01f, 8f)
            }
        }.build()
    }

    /** Иконка камеры */
    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitCamera",
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
                moveTo(23f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 21f)
                lineTo(3f, 21f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 19f)
                lineTo(1f, 9f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 7f)
                lineTo(7f, 7f)
                lineTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(17f, 7f)
                lineTo(21f, 7f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 23f, 9f)
                close()
                moveTo(12f, 11f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 19f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 11f)
                close()
            }
        }.build()
    }

    /** Иконка копирования (Copy) */
    val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitCopy",
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
                moveTo(8f, 4f)
                lineTo(18f, 4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 6f)
                lineTo(20f, 16f)
                moveTo(4f, 8f)
                lineTo(14f, 8f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 10f)
                lineTo(16f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 14f, 22f)
                lineTo(4f, 22f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 20f)
                lineTo(2f, 10f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 8f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitSearch",
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
                moveTo(11f, 19f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = false, 11f, 3f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 11f, 19f)
                close()
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()
    }
}
