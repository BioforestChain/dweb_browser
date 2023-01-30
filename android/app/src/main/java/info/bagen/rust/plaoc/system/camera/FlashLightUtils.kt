package info.bagen.rust.plaoc.system.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.CountDownTimer
import android.widget.Toast
import androidx.annotation.RequiresApi
import info.bagen.rust.plaoc.App

@SuppressLint("StaticFieldLeak")
object FlashLightUtils {
    private val context: Context = App.appContext

    @JvmField
    var isOn = false
    private var timer: Timer? = null

    /**
     * 开启闪光灯
     */
    @JvmStatic
    fun lightOn() = lightOn(false)

    /**
     * 关闭闪光灯
     */
    @JvmStatic
    fun lightOff() = lightOff(false)

    /**
     * 开启sos，speed越大速度越快，建议取值 1-6
     */
    @JvmStatic
    fun sos(speed: Int) {
        //先关闭闪光灯
        lightOff()
        timer?.cancel()
        val countDownInterval = (1500 / speed).toLong()
        timer = Timer(countDownInterval) {
            if (isOn) lightOff(true) else lightOn(true)
        }
        timer?.start()

    }

    /**
     * 关闭闪光灯
     */
    @JvmStatic
    fun offSos() {
        timer?.cancel()
        lightOff(true)
    }


    private val lightOn = { isSos: Boolean ->
        if (!isSos) timer?.cancel()

        if (hasFlashlight()) {
            isOn = true
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) lightOn22()
            else lightOn23()
        } else Toast.makeText(context, "该设备没有闪光灯", Toast.LENGTH_SHORT).show()
    }

    private val lightOff: (Boolean) -> Unit = {
        if (!it) timer?.cancel()
        if (hasFlashlight()) {
            isOn = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) lightOff22()
            else lightOff23()
        } else Toast.makeText(context, "该设备没有闪光灯", Toast.LENGTH_SHORT).show()
    }

    private val lightOn22 = {
        val camera = Camera.open()
        val parameters = camera.parameters
        parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        camera.parameters = parameters
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val lightOn23 = {
        try {
            var manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.setTorchMode("0", true)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private val lightOff22 = {
        val camera = Camera.open()
        val parameters = camera.parameters
        parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
        camera.parameters = parameters
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val lightOff23 = {
        try {
            var manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.setTorchMode("0", false)
        } catch (e: NullPointerException) {
            throw RuntimeException("请先调用init()方法初始化")
        }
    }

    @JvmStatic
    fun hasFlashlight(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    class Timer(countDownInterval: Long, val tick: (Long) -> Unit) :
        CountDownTimer(Long.MAX_VALUE, countDownInterval) {
        override fun onFinish() {
            start()
        }

        override fun onTick(millisUntilFinished: Long) = tick(millisUntilFinished)
    }
}
