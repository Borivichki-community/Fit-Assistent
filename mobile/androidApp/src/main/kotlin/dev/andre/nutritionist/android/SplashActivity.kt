package dev.andre.nutritionist.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Легковесный и высокопроизводительный сплэш-экран приложения «fit-assistent».
 * Реализует 1.5-секундную анимацию в стиле антигравити-минимализма без сторонних библиотек.
 */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvSplashLogo = findViewById<TextView>(R.id.tvSplashLogo)
        val ivSplashLogo = findViewById<ImageView>(R.id.ivSplashLogo)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)

        // Начальное состояние для 50dp смещения
        val offset50dp = 50f * resources.displayMetrics.density

        // 1. Верхний текст (fit-assistent): изначально скрыт и сдвинут вверх на 50dp
        tvSplashLogo.alpha = 0f
        tvSplashLogo.translationY = -offset50dp

        // 2. Нижний текст (Здравствуйте!): изначально скрыт и сдвинут вниз на 50dp
        tvGreeting.alpha = 0f
        tvGreeting.translationY = offset50dp

        // 3. Центральная 3D-иконка яблока: изначально масштаб 1.1f
        ivSplashLogo.scaleX = 1.1f
        ivSplashLogo.scaleY = 1.1f

        // Анимация верхнего текста: за 1200ms проявляется и опускается
        tvSplashLogo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ANIM_TEXT_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Анимация нижнего текста: за 1200ms проявляется и поднимается
        tvGreeting.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ANIM_TEXT_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Анимация центральной иконки: за 1500ms плавно уменьшается до 0.85f (уход вглубь)
        ivSplashLogo.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(TOTAL_SPLASH_TIME_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // По завершении ровно 1500 мс — плавный переход на MainActivity
        lifecycleScope.launch {
            delay(TOTAL_SPLASH_TIME_MS)
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in,
                R.anim.fade_out,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        finish()
    }

    companion object {
        private const val ANIM_TEXT_DURATION_MS = 1200L
        private const val TOTAL_SPLASH_TIME_MS = 1500L
    }
}
