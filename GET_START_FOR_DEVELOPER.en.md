### Environment Setup

> Ensure that you have proper internet access; otherwise, some software downloads may be very slow.
>
> Gradle is not recommended to use mirrors because we rely on the latest official packages, and third-party mirrors often fail to update promptly. If you encounter the `could not resolve all dependencies for configuration` error, check your global Gradle configuration as it might be incorrect.

1. **Install Android Studio**

   - Download the stable version of [Android Studio](https://developer.android.com/studio/).
     - Follow the installation guide, clicking "Next" and agreeing to the terms.
     - Be sure to agree to all licenses; otherwise, you won't be able to proceed.

2. **Install Xcode**:

   - Download from the App Store.
   - If you are on Windows/Linux or do not need to compile iOS projects, you can skip this step.

3. **Install Deno**

   - For Mac users:
     1. Run: `curl -fsSL https://deno.land/x/install/install.sh | sh` in your terminal.
     2. Add Deno to your PATH for easier usage next time. This depends on your terminal environment. For example, if you use zprofile:
        - a. `vim ~/.zprofile`
        - b. Add `export PATH=$PATH:/Users/your_username/.deno/bin` (replace `/Users/your_username/.deno/bin` with your actual Deno installation path).
        - c. Run `source .zprofile`.

4. **Install Node and Package Managers**

   > It is recommended to install Node >=20.

   - Run `npm i -g pnpm`: A package manager often used for toolchain development.
   - Run `npm i -g yarn`: Another package manager, used for direct project development. Android Studio uses it when running `jsTarget`.
     > Ensure the version is 1.22.21 or higher.

5. **Install JDK 17+**

6. **Recommended Software**
   - [Visual Studio Code](https://code.visualstudio.com/download)
   - [GitHub Desktop](https://desktop.github.com/)

### Starting the Project

1. Open the `next/kmp` directory in the project using Android Studio. This directory contains the Dweb Browser code based on Kotlin Multiplatform Project.
   1. The C# version (next/dweb-browser) has been deprecated.
   1. The desktop development version (desktop-dev) is being phased out. We are developing a formal desktop version, so the old version is no longer maintained and is only used internally during this transitional period.

2. Run `deno task dev` in the project root directory. It will automatically pull submodules, install dependencies, and start developer mode.
   - Developer mode automatically pre-compiles and copies resource files to iOS, Android, Desktop, etc., in real time.
   - If Deno runs for a long time, file watching might fail, or subprocesses might exit unexpectedly. In such cases, restart and run `deno task dev` again.

3. Compile and Run:

   - **For Android users**, start the emulator or connect a device via USB for debugging, then run the project.
   - **For iOS users**, if you are using Android Studio, ensure the `Kotlin Multiplatform Mobile` plugin is installed. In `Edit Configurations`, click `+`, and select `iOS Application`:
     1. Select `xcode project file`, with the path `next/kmp/app/iosApp/DwebBrowser.xcworkspace`.
     1. Choose the `scheme` as `DwebBrowser` and set the `configuration` to `Debug`.
     1. Select your `target` as a simulator or a real device. If your device is not detected, reinstall the `Kotlin Multiplatform Mobile` plugin, which usually resolves the issue.
   - **Alternatively, iOS users** can directly open `next/kmp/app/iosApp/DwebBrowser.xcworkspace` in Xcode and run DwebBrowser.
   - **For Desktop users**, start the application using Android Studio. In `Edit Configurations`, click `+` and select `Gradle`:
     1. Select `Run` and enter `desktopApp:desktopRun -DmainClass=MainKt -Djxbrowser.license.key=xxxxxxxxxxxxx --quiet` where `xxxxxxxxxxxxx` is your actual license key.
     1. Set `Gradle project` to kmp.
     1. Click `Apply` and `OK`, then select the created configuration and click Run.

4. Package the Application:

   - **For Desktop users**, follow the same steps as for compile and run, but configure the `-Djxbrowser.license.key` in the packaging command.
   - **For Android developers**, run `./gradlew androidApp:generateBaselineProfile` first. This will generate text files in the `next/kmp/app/androidApp/src/androidMain/generated/baselineProfiles` folder, which help in optimizing the application's performance.
     > Automated testing might face challenges due to WebView technology, requiring fixed coordinates for element selection. The current test uses a Pixel 4 screen size.
   - Android developers need to configure the `key.properties` file in the androidApp folder and run `deno task bundle:android` to generate apk/aab files. The script will automatically set the version number based on the date, with the following rules:
     - If the date changes, the version is automatically upgraded to $major.$today.$patch=0.
     - On the same day, the patch version will not be modified unless you add the `--new` parameter to force an update.

     The output includes:

     - `DwebBrowser_all_v$VERSION.aab`
     - `DwebBrowser_all_v$VERSION.apk`
     - `DwebBrowser_arm64_v$VERSION.aab`
     - `DwebBrowser_arm64_v$VERSION.apk`
     - `DwebBrowser_arm64_v$VERSION_debug.apk`

     These are two release APKs (arm64 only and all architectures) and their corresponding AABs for Google Play. The debug APK is for arm64 architecture.

   - **To package a macOS application**, the general approach is to first package the `.app` folder, then generate a `.pkg` file, and finally upload via Transporter.
     1. Prepare certificate files as detailed in the official documentation [Checking existing certificates](https://github.com/JetBrains/compose-multiplatform/blob/fc90219ad63799fc4cd08ceb57b428948a223b21/tutorials/Signing_and_notarization_on_macOS/README.md#checking-existing-certificates).
        > Two certificates are required: "Mac App Distribution" and "Mac Installer Distribution".
     1. Run the `next/kmp/build-macos.sh` script to package, which will generate `.app` and `.pkg` files in the `kmp/app/desktopApp/build/compose/binaries/main-release/` folder.

### Tips

1. When developing `jsTarget` in Android Studio, if you encounter the error `yarn.lock was changed. Run the 'kotlinUpgradeYarnLock' task to actualize yarn.lock file`, select the first button in the Gradle panel "Execute Gradle Task" and enter `gradle kotlinUpgradeYarnLock`.
2. Gradle scripts often need to handle dependencies. If you're unsure which tasks you can invoke, consider the following:
   1. To list all gradlew tasks, go to the `kmp/next` directory and run: `./gradlew -q :tasks --all > .gradle/all.md`.
   1. To list tasks for a specific project, run: `./gradlew -q helper:tasks --all > .gradle/helper.md`.
3. Avoid opening the project with Fleet, as it may add files to the `DwebBrowser.xcworkspace` folder, causing xcodebuild scripts to fail. If you accidentally opened it with Fleet, manually delete the files in `DwebBrowser.xcworkspace` that start with `[Fleet]`.
4. When using the [composeResources](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-images-resources.html#resource-usage) folder for resource files, run `./gradlew generateComposeResClass` to automatically generate code.
5. Our `BrowserViewModel` is not an Android ViewModel:
   1. Functions in the ViewModel ending with `UI` are meant for Compose and should not be used elsewhere. To use them, get `uiScope` in Compose and call with Effect. Using these functions across multiple threads may cause thread safety issues.
   1. We may replace `MutableStateOf<T>` with `MutableStateFlow<T>` in the future for thread safety. For instance, bookmarks data already uses `MutableStateFlow`, storing read-only maps and lists, making modifications safer.
6. After upgrading Gradle, remember to also upgrade the entry script, e.g., `./gradlew wrapper --gradle-version=8.6`.
7. You can create a `local.properties` file in the kmp directory to configure the `jxbrowser.license.key` field.
8. Avoid making connections in the Runtime bootstrap, as connections depend on the other party’s bootstrap completion, which can lead to deadlocks.
9. The Runtime has its own `coroutineScope`. You can use `getRuntimeScope()` to access it. Typically, you use the `scopeLaunch` and `scopeAsync` functions, equivalent to `scope.launch` and `scope.async`. These functions require a `cancelable` parameter to obtain a job/deferred, which determines whether to cancel the job/deferred during Runtime shutdown.
   > For example, some tasks, like writing data before shutdown, should not be cancelable.