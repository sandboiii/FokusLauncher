package com.lu4p.fokuslauncher.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Walks through first-run onboarding so profile collection and startup benchmarks measure the
 * steady-state home path a real user sees, not the onboarding flow a fresh test install lands on.
 *
 * Button labels are resolved from the TARGET app's own resources via [createPackageContext],
 * so this works on any device locale. Idempotent: when onboarding is already completed no
 * button matches and it returns after one wait.
 *
 * Call BEFORE the measured block; run [launchAndCompleteOnboarding] once per install, then
 * force-stop so measured cold starts begin from a completed-onboarding state.
 */
internal fun completeOnboardingIfShown(device: UiDevice) {
    val targetContext =
            InstrumentationRegistry.getInstrumentation()
                    .context
                    .createPackageContext(TARGET_PACKAGE, 0)

    fun str(name: String): String? {
        val id = targetContext.resources.getIdentifier(name, "string", TARGET_PACKAGE)
        return if (id != 0) targetContext.resources.getString(id) else null
    }

    // One "advance this step" label per onboarding step, in on-screen order. "Keep wallpaper"
    // (not "black") on the background step so collection does not repaint the device wallpaper.
    val advanceLabels =
            listOf(
                            "onboarding_get_started",
                            "onboarding_background_wallpaper",
                            "onboarding_location_skip",
                            "onboarding_skip",
                            "onboarding_next",
                            "onboarding_done",
                    )
                    .mapNotNull(::str)

    // Steps are conditional (e.g. set-default only shows when not default), so instead of a
    // fixed script: click whichever advance button is on screen until none remains (= home).
    repeat(12) {
        val button =
                advanceLabels.firstNotNullOfOrNull { label ->
                    device.wait(Until.findObject(By.text(label)), 1_500)
                } ?: return
        button.click()
        device.waitForIdle()
    }
}

/** Launch the launcher, finish onboarding if present, and force-stop to restore a cold state. */
internal fun launchAndCompleteOnboarding(device: UiDevice) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.context.startActivity(launcherIntent())
    device.waitForIdle()
    completeOnboardingIfShown(device)
    device.executeShellCommand("am force-stop $TARGET_PACKAGE")
    device.waitForIdle()
}
