package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleSignal
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class DesktopWindowController(
  override val context: Context, internal val state: WindowState
) : WindowController() {
  override fun toJson() = state

  val id = state.wid;

  private val _blurSignal = SimpleSignal()
  val onBlur = _blurSignal.toListener()
  private val _focusSignal = SimpleSignal()
  val onFocus = _focusSignal.toListener()
  fun isFocused() = state.focus
  suspend fun focus() {
    if (!state.focus) {
      state.focus = true
      state.emitChange()
      _focusSignal.emit()
    }
  }

  suspend fun blur() {
    if (state.focus) {
      state.focus = false
      state.emitChange()
      _blurSignal.emit()
    }
  }


  @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
  @Composable
  fun Render(modifier: Modifier = Modifier) {
    var winState by remember { mutableStateOf(this.state, neverEqualPolicy()) }
    LaunchedEffect(this.state) {
      launch {
        winState.onChange.toFlow().collect {
          winState = this@DesktopWindowController.state;
        }
      }
    }
    val coroutineScope = rememberCoroutineScope()
    val emitWinStateChange = { -> coroutineScope.launch { winState.emitChange() } }
    val emitWinFocus = { focused: Boolean ->
      coroutineScope.launch {
        if (focused) {
          focus()
        } else {
          blur()
        }
      }
    }
    val density = LocalDensity.current

    /// 窗口移动
    var inMove by remember { mutableStateOf(false) }
    fun Modifier.moveable() = this.pointerInput(Unit) {
      detectDragGestures(onDragStart = {
        inMove = true
        /// 开始移动的时候，同时进行聚焦
        emitWinFocus(true)
      }, onDragEnd = {
        inMove = false
      }) { change, dragAmount ->
        change.consume()
        winState.bounds.left += dragAmount.x / density.density
        winState.bounds.top += dragAmount.y / density.density
        emitWinStateChange()
      }
    }


    /// 窗口缩放计算
    val config = LocalConfiguration.current
    val screenWidth = remember(config) {
      config.screenWidthDp.toFloat()
    };
    val screenHeight = remember(config) {
      config.screenHeightDp.toFloat()
    };
    /**
     * 视图的最小缩放
     */
    val minScale = 0.3;
    val minWidth = screenWidth * 0.2f
    val minHeight = screenHeight * 0.2f

    val boundsWidth = max(winState.bounds.width, minWidth)
    val boundsHeight = max(winState.bounds.height, minHeight)
    val winBounds = winState.bounds.copy(width = boundsWidth, height = boundsHeight);
    if (winBounds != winState.bounds) {
      winState.bounds = winBounds
      emitWinStateChange()
    }

    ElevatedCard(modifier = winBounds
      .toModifier(modifier)
      .focusable(true)
      .onFocusChanged { state ->
        emitWinFocus(state.isFocused)
      }
      .clickable {
        emitWinFocus(true)
      },
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
      ),
      elevation = if (inMove) CardDefaults.elevatedCardElevation(defaultElevation = 20.dp) else CardDefaults.elevatedCardElevation()
    ) {
      Column {
        var titleBarHeight by remember { mutableIntStateOf(0) }
        val bottomBarHeight = 16;
        /// 标题栏
        Box(modifier = Modifier
          .background(MaterialTheme.colorScheme.onPrimaryContainer)
          .fillMaxWidth()
          .onGloballyPositioned { layoutCoordinates ->
            titleBarHeight = layoutCoordinates.size.height
          }
          /// 标题可以窗口拖动
          .moveable()) {
          Text(
            text = this@DesktopWindowController.state.title,
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
          )
        }
        /// 显示内容
        Box(Modifier.weight(1f)) {
          /**
           * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
           * 但这些缩放不是等比的，而是会以一定比例进行换算。
           */
          windowAdapterManager.providers[this@DesktopWindowController.state.wid]?.also {
            /**
             * 计算进度
             */
            fun calcProgress(from: Float, now: Float, to: Float) =
              ((now - from) / (to - from)).toDouble()

            /**
             * 将进度转成动画进度
             */
            fun Double.easeInCirc(): Double {
              return 1.0 - sqrt(1.0 - this.pow(2.0));
            }

            /**
             * 将动画进度还原成所需的缩放值
             */
            fun Double.toScale(minScale: Double, maxScale: Double = 1.0) =
              ((maxScale - minScale) * this) + minScale;


            var viewHeight by remember { mutableFloatStateOf(100f) }
            var viewWidth by remember { mutableFloatStateOf(100f) }
            val viewScale = max(
              calcProgress(screenWidth, viewWidth, minWidth).toScale(minScale),
              calcProgress(screenHeight, viewHeight, minHeight).toScale(minScale),
            ).toFloat().let { if (it.isNaN()) 1f else it }

            println("viewHeight: $viewHeight")
            println("viewWidth: $viewWidth")
            println("viewScale: $viewScale")
            Box(
              Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                  viewWidth = layoutCoordinates.size.width.toFloat()
                  viewHeight = layoutCoordinates.size.height.toFloat()
                }) {
              it(
                Modifier
                  .fillMaxSize()
//                  .graphicsLayer(viewScale, viewScale)
//                  .requiredSize(
//                    (viewWidth / viewScale).toInt().dp, (viewHeight / viewScale).toInt().dp
//                  )
              )
            }
          } ?: Text(
            "Op！视图被销毁了",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineLarge.copy(
              color = MaterialTheme.colorScheme.error,
              background = MaterialTheme.colorScheme.errorContainer
            )
          )

          /// 失去焦点的时候，提供 moveable 的遮罩（在移动中需要确保遮罩存在）
          if (inMove or !winState.focus) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (winState.focus) 0f else 0.2f))
                .moveable()
            )
          }
        }
        /// 显示底部控制条
        Row(
          modifier = Modifier
            .background(Color.Cyan)
            .height(bottomBarHeight.dp)
            .fillMaxWidth()
        ) {
          Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Color.Red)
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                winState.bounds.left += dragAmount.x / density.density
                winState.bounds.width += dragAmount.x / density.density
                winState.bounds.height += dragAmount.y / density.density
                emitWinStateChange()
              }
            })
          Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Color.Green)
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                winState.bounds.height += dragAmount.y / density.density
                emitWinStateChange()
              }
            })
          Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Color.Blue)
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                winState.bounds.width += dragAmount.x / density.density
                winState.bounds.height += dragAmount.y / density.density
                emitWinStateChange()
              }
            })
        }
      }
    }
  }
}