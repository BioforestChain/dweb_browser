package info.bagen.dwebbrowser.microService.browser.desk

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import info.bagen.dwebbrowser.App
import org.dweb_browser.dwebview.DWebView

/**
 * 用于和 Service 之间的交互，显示隐藏等操作
 */
object TaskbarModel : ViewModel() {
  private val TAG = TaskbarModel::class.java.simpleName
  val isShowFloatWindow = MutableLiveData<Boolean>()
  val floatWindowLayoutParams by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val size = 200
    val layoutParams = WindowManager.LayoutParams().apply {
      /**
       * 设置type 这里进行了兼容
       */
      type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      format = PixelFormat.RGBA_8888
      flags =
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      //位置大小设置
      width = size//ViewGroup.LayoutParams.WRAP_CONTENT
      height = size//ViewGroup.LayoutParams.WRAP_CONTENT
      gravity = Gravity.END or Gravity.TOP
      //设置剧中屏幕显示
      //x = outMetrics.widthPixels / 2 - width / 2
      //y = outMetrics.heightPixels / 2 - height / 2
      x = 0
      y = 100
    }
    layoutParams
  }

  private var taskbarController: TaskBarController = DesktopNMM.taskBarController

  val taskbarDWebView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    DWebView(
      context = App.appContext,
      remoteMM = taskbarController.desktopNMM,
      options = DWebView.Options(
        url = taskbarController.getTaskbarUrl().toString(),
        onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
      )
    ).also {
      it.setBackgroundColor(Color.Transparent.toArgb())
    }
  }

  /**
   * 打开悬浮框
   */
  fun openFloatWindow() {
    isShowFloatWindow.postValue(true)
  }

  fun closeFloatWindow() {
    isShowFloatWindow.postValue(false)
  }

  fun openTaskActivity() {
      closeFloatWindow()
      App.appContext.startActivity(Intent(App.appContext, TaskbarActivity::class.java).also {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      })
  }
}

class TaskbarService : LifecycleService() {
  private val TAG = TaskbarService::class.java.simpleName
  private var mWindowManager: WindowManager? = null
  private var mFloatRootView: View? = null//悬浮窗View

  override fun onCreate() {
    super.onCreate()
    initObserve()
  }

  override fun onStartCommand(intent: Intent?, flags: kotlin.Int, startId: kotlin.Int): kotlin.Int {
    TaskbarModel.openFloatWindow()
    return super.onStartCommand(intent, flags, startId)
  }

  private fun initObserve() {
    TaskbarModel.apply {
      /**
       * 悬浮窗按钮的创建和移除
       */
      isShowFloatWindow.observe(this@TaskbarService) {
        Log.e(TAG, "initObserve show->$it")
        if (it) {
          showFloatWindow()
        } else {
          mFloatRootView?.let { rootView ->
            rootView.windowToken?.let {
              mWindowManager?.removeView(rootView)
            }
          }
        }
      }
    }
  }

  @SuppressLint("InflateParams", "ClickableViewAccessibility")
  private fun showFloatWindow() {
    //获取WindowManager
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    mWindowManager = windowManager
    val outMetrics = resources.displayMetrics
    val size = 200 // 悬浮框窗口大小

    mFloatRootView = LinearLayout(this).apply {
      this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

      this.addView(TaskbarModel.taskbarDWebView.also { dwebView ->
        dwebView.parent?.let { parent -> (parent as ViewGroup).removeView(dwebView) }
        dwebView.isHorizontalScrollBarEnabled = false
        dwebView.isVerticalScrollBarEnabled = false
        dwebView.setOnTouchListener(
          ItemViewTouchListener(
            TaskbarModel.floatWindowLayoutParams, windowManager, this, outMetrics, size
          )
        )
        dwebView.setOnClickListener {
          TaskbarModel.closeFloatWindow()
        }
      })
    }
    // 将悬浮窗控件添加到WindowManager
    windowManager.addView(mFloatRootView, TaskbarModel.floatWindowLayoutParams)
  }
}

/**
 * 用于悬浮框的滑动操作
 */
class ItemViewTouchListener(
  private val layoutParams: WindowManager.LayoutParams,
  private val windowManager: WindowManager,
  private val view: LinearLayout,
  private val displayMetrics: DisplayMetrics,
  private val size: Int, // 悬浮框大小
) : View.OnTouchListener {
  private var x = 0
  private var y = 0

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(v: View, motionEvent: MotionEvent): Boolean {
    when (motionEvent.action) {
      MotionEvent.ACTION_DOWN -> {
        x = motionEvent.rawX.toInt()
        y = motionEvent.rawY.toInt()
      }

      MotionEvent.ACTION_MOVE -> {
        // Log.e("lin.huang", "ACTION_MOVE :: ($x -> ${motionEvent.rawX}), (${view.left},${view.top},${view.right},${view.bottom})")
        // Log.e("lin.huang", "ACTION_MOVE :: ($y -> ${motionEvent.rawY}), (${layoutParams.x},${layoutParams.y})")
        val nowX = motionEvent.rawX.toInt()
        val nowY = motionEvent.rawY.toInt()
        val movedX = nowX - x
        val movedY = nowY - y
        x = nowX
        y = nowY
        layoutParams.apply {
          // 由于window设置的gravity是END和TOP，所以默认右上角的坐标是(0,0)，导致x移动需要-变化量，y移动需要+变化量
          this.x -= movedX
          this.y += movedY
        }
        //更新悬浮球控件位置
        windowManager.updateViewLayout(view, layoutParams)
      }

      MotionEvent.ACTION_UP -> {
        // 判断如果时间小于200ms，属于点击事件
        if (motionEvent.eventTime - motionEvent.downTime <= 200) {
          // 触发了点击事件。
          TaskbarModel.closeFloatWindow()
          TaskbarModel.openTaskActivity()
        } else {
          // 触发贴边操作
          animSlide()
        }
      }

      else -> {
      }
    }
    return true
  }

  /**
   * 贴边操作
   */
  @SuppressLint("Recycle")
  private fun animSlide() {
    val xFrom = layoutParams.x
    val xTo = if (layoutParams.x > (displayMetrics.widthPixels - size) / 2) {
      displayMetrics.widthPixels - size
    } else {
      0
    }
    // 修复y的左边，避免越界后，上滑坐标值不同步问题
    if (layoutParams.y < 0) {
      layoutParams.y = 0
    } else if (layoutParams.y > displayMetrics.heightPixels - size) {
      layoutParams.y = displayMetrics.heightPixels - size
    }
    // Log.e("lin.huang", "animSlide -> ($xFrom, $xTo), ${displayMetrics.widthPixels}")
    val valueAnimator = ValueAnimator.ofInt(xFrom, xTo)
    valueAnimator.addUpdateListener {
      val xPoint = it.animatedValue as Int
      layoutParams.x = xPoint
      windowManager.updateViewLayout(view, layoutParams)
    }
    valueAnimator.setDuration(500)
    valueAnimator.start()
  }
}