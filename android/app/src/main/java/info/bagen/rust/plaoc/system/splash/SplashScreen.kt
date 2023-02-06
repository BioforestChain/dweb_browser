package info.bagen.rust.plaoc.system.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources.NotFoundException
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.R

class SplashScreen(
    private var context: Context = App.appContext, private var config: SplashScreenConfig
) {
    private val TAG = "SplashScreen"

    companion object {
        private val sInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SplashScreen(App.appContext, getSplashScreenConfig())
        }

        fun load() {
            App.dwebViewActivity?.let { activity ->
                sInstance.showOnLaunch(activity = activity)
            }
        }

        fun show(
            showDuration: Long? = null,
            fadeInDuration: Long? = null,
            fadeOutDuration: Long? = null,
            autoHide: Boolean = true,
            onErrorCallback: ((String) -> Unit)? = null
        ) {
            val settings = SplashScreenSettings(
                showDuration = showDuration ?: 3000L,
                fadeInDuration = fadeInDuration ?: 200L,
                fadeOutDuration = fadeOutDuration ?: 200L,
                autoHide = autoHide
            )
            App.dwebViewActivity?.let { activity ->
                sInstance.show(activity, settings, onErrorCallback)
            }
        }

        fun hide() {
            App.dwebViewActivity?.let { activity ->
                if (sInstance.config.usingDialog) {
                    sInstance.hideDialog(activity = activity)
                } else if (sInstance.settings != null) {
                    sInstance.hide(sInstance.settings!!)
                }
            }

        }

        private fun getSplashScreenConfig(): SplashScreenConfig {
            return SplashScreenConfig()
        }
    }

    private var dialog: Dialog? = null
    private var settings: SplashScreenSettings? = null
    private var splashImage: View? = null
    private var spinnerBar: ProgressBar? = null
    private var windowManager: WindowManager? = null
    private var isVisible = false
    private var isHiding = false
    private var content: View? = null
    private var onPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

    /**
     * Show the splash screen on launch without fading in
     *
     * @param activity
     */
    fun showOnLaunch(activity: Activity) {
        if (config.launchShowDuration == 0) {
            return
        }
        val settings = SplashScreenSettings()
        settings.showDuration = config.launchShowDuration.toLong()
        settings.autoHide = config.launchAutoHide

        // Method can fail if styles are incorrectly set...
        // If it fails, log error & fallback to old method
        try {
            showWithAndroid12API(activity, settings)
            return
        } catch (e: Exception) {
            Log.e(TAG, "Android 12 Splash API failed... using previous method.")
            onPreDrawListener = null
        }
        settings.fadeInDuration = config.launchFadeInDuration.toLong()
        if (config.usingDialog) {
            showDialog(activity, settings, true, null)
        } else {
            show(activity, settings, true, null)
        }
    }

    /**
     * Show the Splash Screen using the Android 12 API (31+)
     * Uses Compat Library for backwards compatibility
     *
     * @param activity
     * @param settings Settings used to show the Splash Screen
     */
    private fun showWithAndroid12API(activity: Activity, settings: SplashScreenSettings) {
        if (activity.isFinishing) return
        activity.runOnUiThread {
            val windowSplashScreen = activity.installSplashScreen()
            windowSplashScreen.setKeepOnScreenCondition { isVisible || isHiding }

            // Set Fade Out Animation
            windowSplashScreen.setOnExitAnimationListener { windowSplashScreenView ->
                val fadeAnimator: ObjectAnimator =
                    ObjectAnimator.ofFloat(windowSplashScreenView.view, View.ALPHA, 1f, 0f)
                fadeAnimator.interpolator = LinearInterpolator()
                fadeAnimator.duration = settings.fadeOutDuration
                fadeAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isHiding = false
                        windowSplashScreenView.remove()
                    }
                })
                fadeAnimator.start()
                isHiding = true
                isVisible = false
            }

            // Set Pre Draw Listener & Delay Drawing Until Duration Elapses
            content = activity.findViewById(android.R.id.content)
            onPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Start Timer On First Run
                    if (!isVisible && !isHiding) {
                        isVisible = true
                        Handler(context.mainLooper).postDelayed(
                            {
                                // Splash screen is done... start drawing content.
                                if (settings.autoHide) {
                                    isVisible = false
                                    onPreDrawListener = null
                                    content!!.viewTreeObserver.removeOnPreDrawListener(this)
                                }
                            }, settings.showDuration
                        )
                    }

                    // Not ready to dismiss splash screen
                    return false
                }
            }
            content!!.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
        }
    }

    /**
     * Show the Splash Screen
     *
     * @param activity
     * @param settings Settings used to show the Splash Screen
     * @param splashListener A listener to handle the finish of the animation (if any)
     */
    fun show(
        activity: Activity, settings: SplashScreenSettings, onErrorCallback: ((String) -> Unit)?
    ) {
        this.settings = settings
        if (config.usingDialog) {
            showDialog(activity, settings, false, onErrorCallback)
        } else {
            show(activity, settings, false, onErrorCallback)
        }
    }

    private fun showDialog(
        activity: Activity?,
        settings: SplashScreenSettings,
        isLaunchSplash: Boolean,
        onErrorCallback: ((String) -> Unit)?
    ) {
        if (activity == null || activity.isFinishing) return
        if (isVisible) {
            onErrorCallback?.let { it("OnCompleted") }
            return
        }
        activity.runOnUiThread {
            dialog = if (config.immersive) {
                Dialog(activity, R.style.capacitor_immersive_style)
            } else if (config.fullScreen) {
                Dialog(activity, R.style.capacitor_full_screen_style)
            } else {
                Dialog(activity, R.style.capacitor_default_style)
            }
            val splashId =
                context.resources.getIdentifier(config.layoutName, "layout", context.packageName)
            if (splashId == 0) {
                Log.e(TAG, "Layout not found, using default")
            }
            if (splashId != 0) {
                dialog!!.setContentView(splashId)
            } else {
                val splash: Drawable? = getSplashDrawable()
                val parent = LinearLayout(context)
                parent.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                parent.orientation = LinearLayout.VERTICAL
                if (splash != null) {
                    parent.background = splash
                }
                dialog!!.setContentView(parent)
            }
            dialog!!.setCancelable(false)
            if (!dialog!!.isShowing) {
                dialog!!.show()
            }
            isVisible = true
            if (settings.autoHide) {
                Handler(context.mainLooper).postDelayed(
                    {
                        hideDialog(activity, isLaunchSplash)
                        onErrorCallback?.let { it("OnCompleted") }
                    }, settings.showDuration
                )
            } else {
                // If no autoHide, call complete
                onErrorCallback?.let { it("OnCompleted") }
            }
        }
    }

    /**
     * Hide the Splash Screen
     *
     * @param settings Settings used to hide the Splash Screen
     */
    fun hide(settings: SplashScreenSettings) {
        hide(settings.fadeOutDuration, false)
    }

    /**
     * Hide the Splash Screen when showing it as a dialog
     *
     * @param activity the activity showing the dialog
     */
    fun hideDialog(activity: Activity) {
        hideDialog(activity, false)
    }

    fun onPause() {
        tearDown(true)
    }

    fun onDestroy() {
        tearDown(true)
    }

    @SuppressLint("DiscouragedApi")
    private fun buildViews() {
        if (splashImage == null) {
            var splashId = 0
            val splash: Drawable?
            splashId =
                context.resources.getIdentifier(config.layoutName, "layout", context.packageName)
            if (splashId == 0) {
                Log.e(TAG, "Layout not found, defaulting to ImageView")
            }
            if (splashId != 0) {
                val activity = context as Activity?
                val inflator = activity!!.layoutInflater
                val root: ViewGroup = FrameLayout(context)
                root.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                splashImage = inflator.inflate(splashId, root, false)
            } else {
                splash = getSplashDrawable()
                if (splash != null) {
                    if (splash is Animatable) {
                        (splash as Animatable).start()
                    }
                    if (splash is LayerDrawable) {
                        for (i in 0 until splash.numberOfLayers) {
                            val layerDrawable = splash.getDrawable(i)
                            if (layerDrawable is Animatable) {
                                (layerDrawable as Animatable).start()
                            }
                        }
                    }
                    splashImage = ImageView(context)
                    // Stops flickers dead in their tracks
                    // https://stackoverflow.com/a/21847579/32140
                    val imageView = splashImage as ImageView
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    } else {
                        legacyStopFlickers(imageView)
                    }
                    imageView.scaleType = config.scaleType
                    imageView.setImageDrawable(splash)
                }
            }
            splashImage?.fitsSystemWindows = true
            splashImage?.setBackgroundColor(config.backgroundColor.toInt())
        }
        if (spinnerBar == null) {
            val spinnerBarStyle = config.spinnerStyle
            spinnerBar = ProgressBar(context, null, spinnerBarStyle)
            spinnerBar!!.isIndeterminate = true
            val spinnerBarColor: Int = config.spinnerColor.toInt()
            val states = arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_pressed)
            )
            val colors =
                intArrayOf(spinnerBarColor, spinnerBarColor, spinnerBarColor, spinnerBarColor)
            val colorStateList = ColorStateList(states, colors)
            spinnerBar!!.indeterminateTintList = colorStateList
        }
    }

    private fun legacyStopFlickers(imageView: ImageView) {
        imageView.isDrawingCacheEnabled = true
    }

    @SuppressLint("DiscouragedApi", "UseCompatLoadingForDrawables")
    private fun getSplashDrawable(): Drawable? {
        val splashId: Int =
            context.resources.getIdentifier(config.resourceName, "drawable", context.packageName)
        return try {
            context.resources.getDrawable(splashId, context.theme)
        } catch (ex: NotFoundException) {
            Log.e(TAG, "No splash screen found, not displaying")
            null
        }
    }

    private fun show(
        activity: Activity,
        settings: SplashScreenSettings,
        isLaunchSplash: Boolean,
        onErrorCallback: ((String) -> Unit)?,
    ) {
        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (activity.isFinishing) {
            return
        }
        buildViews()
        if (isVisible) {
            onErrorCallback?.let { it("OnCompleted") }
            return
        }
        val listener: Animator.AnimatorListener = object : Animator.AnimatorListener {
            override fun onAnimationEnd(animator: Animator) {
                isVisible = true
                if (settings.autoHide) {
                    Handler(context.mainLooper).postDelayed(
                        {
                            hide(settings.fadeOutDuration, isLaunchSplash)
                            onErrorCallback?.let { it("OnCompleted") }
                        }, settings.showDuration
                    )
                } else {
                    // If no autoHide, call complete
                    onErrorCallback?.let { it("OnCompleted") }
                }
            }

            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
            override fun onAnimationStart(animator: Animator) {}
        }
        val mainHandler = Handler(context.mainLooper)
        mainHandler.post {
            val params: WindowManager.LayoutParams = WindowManager.LayoutParams()
            params.gravity = Gravity.CENTER
            params.flags = activity.window.attributes.flags

            // Required to enable the view to actually fade
            params.format = PixelFormat.TRANSLUCENT
            try {
                windowManager!!.addView(splashImage, params)
            } catch (ex: IllegalStateException) {
                Log.e(TAG, "Could not add splash view")
                return@post
            } catch (ex: IllegalArgumentException) {
                Log.e(TAG, "Could not add splash view")
                return@post
            }
            if (config.immersive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.runOnUiThread {
                        val window: Window = activity.window
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        splashImage?.windowInsetsController?.apply {
                            hide(WindowInsetsCompat.Type.systemBars())
                            systemBarsBehavior =
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    }
                } else {
                    legacyImmersive()
                }
            } else if (config.fullScreen) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.runOnUiThread {
                        val window: Window = activity.window
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        val controller = splashImage?.windowInsetsController
                        controller?.hide(WindowInsetsCompat.Type.statusBars())
                    }
                } else {
                    legacyFullscreen()
                }
            }
            splashImage?.alpha = 0f
            splashImage?.animate()?.alpha(1f)?.setInterpolator(LinearInterpolator())
                ?.setDuration(settings.fadeInDuration)?.setListener(listener)?.start()
            splashImage?.visibility = View.VISIBLE
            if (spinnerBar != null) {
                spinnerBar!!.visibility = View.INVISIBLE
                if (spinnerBar!!.parent != null) {
                    windowManager!!.removeView(spinnerBar)
                }
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                windowManager!!.addView(spinnerBar, params)
                if (config.showSpinner) {
                    spinnerBar!!.alpha = 0f
                    spinnerBar!!.animate().alpha(1f).setInterpolator(LinearInterpolator())
                        .setDuration(settings.fadeInDuration).start()
                    spinnerBar!!.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun legacyImmersive() {
        val flags: Int =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        splashImage?.systemUiVisibility = flags
    }

    private fun legacyFullscreen() {
        splashImage?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }

    private fun hide(fadeOutDuration: Long, isLaunchSplash: Boolean) {
        // Warn the user if the splash was hidden automatically, which means they could be experiencing an app
        // that feels slower than it actually is.
        if (isLaunchSplash && isVisible) {
            Log.e(
                TAG,
                "SplashScreen was automatically hidden after the launch timeout. " + "You should call `SplashScreen.hide()` as soon as your web app is loaded (or increase the timeout)." + "Read more at https://capacitorjs.com/docs/apis/splash-screen#hiding-the-splash-screen"
            )
        }
        if (isHiding) {
            return
        }

        // Hide with Android 12 API
        if (null != onPreDrawListener) {
            isVisible = false
            if (null != content) {
                content!!.viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
            }
            onPreDrawListener = null
            return
        }
        if (splashImage == null || splashImage!!.parent == null) {
            return
        }
        isHiding = true
        val listener: Animator.AnimatorListener = object : Animator.AnimatorListener {
            override fun onAnimationEnd(animator: Animator) {
                tearDown(false)
            }

            override fun onAnimationCancel(animator: Animator) {
                tearDown(false)
            }

            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        }
        val mainHandler = Handler(context.mainLooper)
        mainHandler.post {
            if (spinnerBar != null) {
                spinnerBar!!.alpha = 1f
                spinnerBar!!.animate().alpha(0f).setInterpolator(LinearInterpolator())
                    .setDuration(fadeOutDuration.toLong()).start()
            }
            splashImage!!.alpha = 1f
            splashImage!!.animate().alpha(0f).setInterpolator(LinearInterpolator())
                .setDuration(fadeOutDuration.toLong()).setListener(listener).start()
        }
    }

    private fun hideDialog(activity: Activity, isLaunchSplash: Boolean) {
        // Warn the user if the splash was hidden automatically, which means they could be experiencing an app
        // that feels slower than it actually is.
        if (isLaunchSplash && isVisible) {
            Log.e(
                TAG,
                ("SplashScreen was automatically hidden after the launch timeout. " + "You should call `SplashScreen.hide()` as soon as your web app is loaded (or increase the timeout)." + "Read more at https://capacitorjs.com/docs/apis/splash-screen#hiding-the-splash-screen")
            )
        }
        if (isHiding) {
            return
        }

        // Hide with Android 12 API
        if (null != onPreDrawListener) {
            isVisible = false
            if (null != content) {
                content!!.viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
            }
            onPreDrawListener = null
            return
        }
        isHiding = true
        activity.runOnUiThread {
            if (dialog != null && dialog!!.isShowing) {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    dialog!!.dismiss()
                }
                dialog = null
                isHiding = false
                isVisible = false
            }
        }
    }

    private fun tearDown(removeSpinner: Boolean) {
        if (spinnerBar != null && spinnerBar!!.parent != null) {
            spinnerBar!!.visibility = View.INVISIBLE
            if (removeSpinner) {
                windowManager!!.removeView(spinnerBar)
            }
        }
        if (splashImage != null && splashImage!!.parent != null) {
            splashImage!!.visibility = View.INVISIBLE
            windowManager!!.removeView(splashImage)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && config.fullScreen || config.immersive) {
            // Exit fullscreen mode
            val window: Window = (context as Activity?)!!.window
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
        isHiding = false
        isVisible = false
    }
}

data class SplashScreenSettings(
    var showDuration: Long = 3000L,
    var fadeInDuration: Long = 200L,
    var fadeOutDuration: Long = 200L,
    var autoHide: Boolean = true,
)

data class SplashScreenConfig(
    var backgroundColor: Long = 0xffffffff,
    var spinnerStyle: Int = android.R.attr.progressBarStyleInverse,
    var spinnerColor: Long = 0xffffffff,
    var showSpinner: Boolean = false,
    var launchShowDuration: Int = 500,
    var launchAutoHide: Boolean = true,
    var launchFadeInDuration: Int = 0,
    var resourceName: String = "splash",
    var immersive: Boolean = false,
    var fullScreen: Boolean = false,
    var scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_XY,
    var usingDialog: Boolean = false,
    var layoutName: String = "launch_screen",
)
