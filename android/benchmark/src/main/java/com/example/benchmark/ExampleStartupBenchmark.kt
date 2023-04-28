package com.example.benchmark

import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>` tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "info.bagen.dwebbrowser",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}

/**
 * 产生 baseline profile 文件
 * 路径 build/outputs/connected_android_test_additional_output/benchmark/connected/AVD/BaselineProfileGenerator_startup-baseline-prof-2023-02-14-09-39-28.txt
 */
@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() =
        baselineProfileRule.collectBaselineProfile(packageName = "info.bagen.dwebbrowser") {
            pressHome()
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll
            // through your most important UI.
            startActivityAndWait()
        }
}

@RunWith(AndroidJUnit4::class)
class BaselineProfileBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 完全不进行编译
     */
    @Test
    fun startupNoCompilation() {
        startup(CompilationMode.None())
    }

    /**
     * 完全AOT编译
     */
    @Test
    fun startupBaselineFull() = startup(
        CompilationMode.Full()
    )

    /**
     * 根据配置文件进行编译
     */
    @Test
    fun startupBaselineProfile() {
        startup(
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Require
            )
        )
    }

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "info.bagen.dwebbrowser",
            metrics = listOf(StartupTimingMetric()),
            iterations = 10,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode
        ) { // this = MacrobenchmarkScope
            pressHome()
            startActivityAndWait()
        }
    }
}