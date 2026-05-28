package com.example

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMainActivityLaunchAndInteraction() {
    // Pre-populate SharedPreferences to bypass login screen
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("event_planner_prefs", Context.MODE_PRIVATE)
    sharedPrefs.edit()
      .putBoolean("is_authenticated", true)
      .putString("user_name", "John Doe")
      .putString("user_email", "john.doe@university.edu")
      .apply()

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      assertNotNull(scenario)
      scenario.onActivity { activity ->
        // Assert layout main binding is active and visible
        val layoutMainApp = activity.findViewById<View>(R.id.layoutMainApp)
        assertNotNull(layoutMainApp)
        assertEquals(View.VISIBLE, layoutMainApp.visibility)

        // Switch to Settings screen
        val navSettings = activity.findViewById<View>(R.id.navSettings)
        assertNotNull(navSettings)
        navSettings.performClick()

        val layoutSettings = activity.findViewById<View>(R.id.layoutSettings)
        assertNotNull(layoutSettings)
        assertEquals(View.VISIBLE, layoutSettings.visibility)

        // Toggle dark mode switch
        val switchDarkMode = activity.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchDarkMode)
        if (switchDarkMode != null) {
            switchDarkMode.performClick()
        } else {
            val matSwitch = activity.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchDarkMode)
            assertNotNull(matSwitch)
            matSwitch.performClick()
        }

        // Switch back to Events screen
        val navEvents = activity.findViewById<View>(R.id.navEvents)
        assertNotNull(navEvents)
        navEvents.performClick()
        assertEquals(View.VISIBLE, layoutMainApp.visibility)

        // Click Category Filter
        val chipWorkshop = activity.findViewById<View>(R.id.chipWorkshop)
        if (chipWorkshop != null) {
            chipWorkshop.performClick()
        }

        // Check if stats mini cards display correctly
        val tvStatTotal = activity.findViewById<TextView>(R.id.tvStatTotal)
        assertNotNull(tvStatTotal)
      }
    }
  }
}
