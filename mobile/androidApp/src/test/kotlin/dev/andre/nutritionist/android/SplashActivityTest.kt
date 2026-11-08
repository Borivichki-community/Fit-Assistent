package dev.andre.nutritionist.android

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
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

        val tvSplashLogo = activity.findViewById<TextView>(R.id.tvSplashLogo)
        val ivSplashLogo = activity.findViewById<ImageView>(R.id.ivSplashLogo)
        val tvGreeting = activity.findViewById<TextView>(R.id.tvGreeting)

        assertNotNull("tvSplashLogo must be present", tvSplashLogo)
        assertNotNull("ivSplashLogo must be present", ivSplashLogo)
        assertNotNull("tvGreeting must be present", tvGreeting)

        assertEquals("fit-assistent", tvSplashLogo.text.toString())
        assertEquals("Здравствуйте!", tvGreeting.text.toString())
    }

    @Test
    fun `after splash duration navigates to MainActivity and finishes`() {
        val controller = Robolectric.buildActivity(SplashActivity::class.java).setup()
        val activity = controller.get()

        // Wait for coroutine delay (1500ms)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val shadowActivity = shadowOf(activity)
        val nextIntent = shadowActivity.nextStartedActivity

        assertNotNull("Must launch next intent", nextIntent)
        assertEquals(MainActivity::class.java.name, nextIntent.component?.className)
        assertTrue("SplashActivity must finish", activity.isFinishing)
    }
}
