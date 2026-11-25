package dev.andre.nutritionist.android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.navigation.AppDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Адаптивный сплэш-экран приложения «Fit Assistant» с поддержкой светлой и тёмной тем.
 * Выполняет проверку статуса авторизации и бесшовный переход в [MainActivity].
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivSplashLogo)
        val tvTitle = findViewById<TextView>(R.id.tvSplashTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvSplashSubtitle)
        val progressIndicator = findViewById<LinearProgressIndicator>(R.id.progressIndicator)

        // Начальное состояние для плавного появления
        ivLogo.alpha = 0f
        ivLogo.scaleX = 0.95f
        ivLogo.scaleY = 0.95f

        tvTitle.alpha = 0f
        tvTitle.scaleX = 0.95f
        tvTitle.scaleY = 0.95f

        tvSubtitle.alpha = 0f
        tvSubtitle.scaleX = 0.95f
        tvSubtitle.scaleY = 0.95f

        progressIndicator?.alpha = 0f

        // Анимация 0 -> 1 и масштаб 0.95 -> 1.0 за 600 мс с DecelerateInterpolator
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIM_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvTitle.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIM_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvSubtitle.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIM_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        progressIndicator?.animate()
            ?.alpha(1f)
            ?.setDuration(ANIM_DURATION_MS)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()

        // Проверка авторизации и переход через 1.2 секунды
        lifecycleScope.launch {
            val isAuth = runCatching { authRepository.isAuthenticated() }.getOrDefault(defaultValue = false)
            delay(SPLASH_DURATION_MS)
            navigateToMain(isAuth)
        }
    }

    private fun navigateToMain(isAuth: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            val destination = if (isAuth) {
                AppDestination.Dashboard.route
            } else {
                AppDestination.Auth.route
            }
            putExtra(MainActivity.EXTRA_START_DESTINATION, destination)
        }
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
        private const val ANIM_DURATION_MS = 600L
        private const val SPLASH_DURATION_MS = 1200L
    }
}
