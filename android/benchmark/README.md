# BaseLine Profile 基准配置文件

## 各种 Android 版本的编译行为
Android 平台版本使用了不同的应用编译方法，每种方法都有相应的性能权衡。基准配置文件提供了一个适用于所有安装的配置文件，对之前的编译方法进行了改进。

| Android 版本 | 编译方法 | 优化方法
|-----|-------|-----
|Android 5（API 级别 21）到 Android 6（API 级别 23）| 完全 AOT | 整个应用会在安装期间进行优化，这会导致用户需要等待较长的时间才能使用应用、RAM 和磁盘空间使用量增加，并且从磁盘加载代码需要更长的时间，进而可能增加冷启动时间。
|Android 7（API 级别 24）到 Android 8.1（API 级别 27）|部分 AOT（基准配置文件）|基准配置文件由 androidx.profileinstaller 在应用首次运行时安装，届时应用模块会定义此依赖项。ART 可以通过以下方法改进这一点：在应用使用期间添加额外的配置文件规则，并在设备空闲时编译这些规则。这可以进行相应优化，以便更好地利用磁盘空间并缩短从磁盘加载代码所需的时间，从而可减少用户使用应用需要等待的时间。
|Android 9（API 级别 28）及更高版本|部分 AOT（基准配置文件 + 云配置文件）|在应用安装期间，Play 会使用基准配置文件优化 APK 和云配置文件（如果有）。应用安装后，ART 配置文件会上传到 Play 并汇总在一起，然后在其他用户安装/更新应用时，以云配置文件的形式提供给他们。

## 创建基准配置文件
1. 确认 Android Studio 版本是2022之后的版本
2. 界面: File -> New -> New Module -> 【Benchmark】。执行该操作会做如下事宜：
```Kotlin
// 在 App 目录下的 build.gradle 添加了如下内容
  buildTypes {
    ...
    benchmark {
        signingConfig signingConfigs.debug
        matchingFallbacks = ['release']
        debuggable false
    }
  }

// 在 App 目录下的 build.gradle 添加了如下内容
    <application>
        <profileable
            android:shell="true"
            tools:targetApi="29" />
    </application>

// 在根目录的 settings.gradle 增加如下内容
include ':benchmark'

// 新增 benchmark module
// 在 Benchmark Module 下新增了 ExampleStartupBenchmark.kt
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
```
3. 手动在 ExampleStartupBenchmark.kt 中添加如下代码
```kotlin
@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() =
        baselineProfileRule.collectBaselineProfile(packageName = "info.bagen.rust.plaoc") {
            pressHome()
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll
            // through your most important UI.
            startActivityAndWait()
        }
}
```
4. 新建一个Android9以上版本模拟器（真机不行），注意系统选择**不包含Google Api**的，执行adb root命令，修改ndk filter添加支持
```kotlin
// 在 app/build.gradle 中
ndk.abiFilters = ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"]
```
5. 最后执行 BaselineProfileGenerator 中的 startup test。在 startup() 位置右击，选择 Run， 记得设备要选择模拟器。执行完成后会在  
**benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/设备名字**  
的位置产生名字类似  
**BaselineProfileGenerator_startup-baseline-prof-2023-02-14-09-39-28.txt**
6. 将该文件拷贝到 App/src/main 目录下，并且改名为 **baseline-prof.txt**

## 验证优化效果
1. 需要添加如下的依赖
```kotlin
  // 安装的时候将 baseline-prof.txt 同步安装
  implementation 'androidx.profileinstaller:profileinstaller:1.2.0-alpha02'
```
2. 手动在 ExampleStartupBenchmark.kt 中添加如下代码，然后分别在真机上面执行下面的三个Test进行验证
```kotlin
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
            packageName = "info.bagen.rust.plaoc",
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
```