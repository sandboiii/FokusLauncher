package com.lu4p.fokuslauncher.baselineprofile

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Collects the baseline profile for the launcher's cold-start critical path.
 *
 * Fokus IS a launcher, so `pressHome()` would open whatever launcher is currently the default —
 * the target activity is always started with an explicit component intent instead.
 *
 * Run on a connected device (API 28+; profile collection without root needs API 33+):
 * `./gradlew :app:generateBaselineProfile`
 * then commit the emitted text profiles under `app/src/release/generated/baselineProfiles`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // Finish onboarding BEFORE collecting so the startup profile captures the home path,
        // not the onboarding flow a fresh install lands on.
        launchAndCompleteOnboarding(
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        )
        rule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
        ) {
            startActivityAndWait(launcherIntent())
            device.waitForIdle()

            // Open the app drawer so its composition lands in the profile too.
            val w = device.displayWidth
            val h = device.displayHeight
            device.swipe(w / 2, (h * 0.8).toInt(), w / 2, (h * 0.2).toInt(), 20)
            device.waitForIdle()

            // Exercise drawer search: type "Ozon" one letter at a time so the per-keystroke
            // filtering/recomposition path is profiled, not just the final query. The field is
            // re-found each step: recomposition can invalidate the node, and single-match
            // auto-launch may close the drawer mid-loop (then the field is simply gone).
            device.wait(Until.findObject(By.clazz("android.widget.EditText")), 3_000)?.let {
                it.click()
                device.waitForIdle()
                val query = "Ozon"
                for (i in 1..query.length) {
                    val field =
                            device.findObject(By.clazz("android.widget.EditText")) ?: break
                    field.text = query.substring(0, i)
                    device.waitForIdle()
                }
            }

            device.pressBack() // dismiss keyboard / clear search
            device.pressBack() // close drawer
            device.waitForIdle()
        }
    }
}

internal const val TARGET_PACKAGE = "io.github.luantak.fokuslauncher"

internal fun launcherIntent(): Intent =
        Intent(Intent.ACTION_MAIN)
                .setComponent(
                        ComponentName(TARGET_PACKAGE, "com.lu4p.fokuslauncher.MainActivity")
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
