package dev.andre.nutritionist.android

import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SplashActivityTest {

    @Test
    fun `splash activity sets up views and animates properly`() {
        val controller = Robolectric.buildActivity(SplashActivity::class.java).setup()
        val activity = controller.get()

        val ivSplashLogo = activity.findViewById<ImageView>(R.id.ivSplashLogo)
        val tvSplashTitle = activity.findViewById<TextView>(R.id.tvSplashTitle)
        val tvSplashSubtitle = activity.findViewById<TextView>(R.id.tvSplashSubtitle)
        val progressIndicator = activity.findViewById<LinearProgressIndicator>(R.id.progressIndicator)

        assertNotNull("ivSplashLogo must be present", ivSplashLogo)
        assertNotNull("tvSplashTitle must be present", tvSplashTitle)
        assertNotNull("tvSplashSubtitle must be present", tvSplashSubtitle)
        assertNotNull("progressIndicator must be present", progressIndicator)

        assertEquals("Fit Assistant", tvSplashTitle.text.toString())
    }

    @Test
    fun `after splash duration navigates to MainActivity and finishes`() {
        val controller = Robolectric.buildActivity(SplashActivity::class.java).setup()
        val activity = controller.get()

        // Wait for coroutine delay (1200ms)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val shadowActivity = shadowOf(activity)
        val nextIntent = shadowActivity.nextStartedActivity

        assertNotNull("Must launch next intent", nextIntent)
        assertEquals(MainActivity::class.java.name, nextIntent.component?.className)
        assertTrue("SplashActivity must finish", activity.isFinishing)
    }
}
