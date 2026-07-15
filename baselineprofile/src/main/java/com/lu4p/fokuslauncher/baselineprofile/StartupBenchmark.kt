package com.lu4p.fokuslauncher.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold-start benchmark: `timeToInitialDisplayMs` plus `timeToFullDisplayMs` (driven by the
 * `ReportDrawnWhen` in HomeScreen, i.e. "home text is visible").
 *
 * `startupCompilationNone` vs `startupCompilationBaselineProfiles` quantifies the profile's win:
 * `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun completeOnboarding() {
        launchAndCompleteOnboarding(
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        )
    }

    @Test
    fun startupCompilationNone() = startup(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() =
            startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) =
            benchmarkRule.measureRepeated(
                    packageName = TARGET_PACKAGE,
                    metrics = listOf(StartupTimingMetric()),
                    compilationMode = compilationMode,
                    startupMode = StartupMode.COLD,
                    iterations = 10,
            ) {
                startActivityAndWait(launcherIntent())
            }
}
