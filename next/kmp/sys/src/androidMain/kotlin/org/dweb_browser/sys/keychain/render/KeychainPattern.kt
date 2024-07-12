package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.jvmSha256

@Preview
@Composable
fun RegisterPatternPreview() {
  val viewModel = remember { RegisterPatternViewModel(CompletableDeferred()) }
  RegisterPattern(viewModel, Modifier.fillMaxSize())
}

@Composable
fun RegisterPattern(
  viewModel: RegisterPatternViewModel,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    CardTitle("请设置图案")
    viewModel.tipMessage?.also { tipMessage ->
      CardDescription(
        tipMessage.tip,
        style = if (tipMessage.isError) TextStyle(color = MaterialTheme.colorScheme.error) else null
      )
    }
    CommonPattern(viewModel, Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
    Row(Modifier.align(Alignment.End).padding(vertical = 8.dp)) {
      if (viewModel.firstConfirmed) {
        FilledTonalButton({
          viewModel.restart()
        }) {
          Text("重绘")
        }
        FilledTonalButton({
          viewModel.confirm()
        }, enabled = viewModel.secondConfirmed) {
          Text("确定")
        }
      } else {
        FilledTonalButton({ viewModel.confirmFirst() }, enabled = viewModel.firstPassword != null) {
          Text("下一步")
        }
      }
    }
  }
}

@Composable
fun VerifyPattern(
  viewModel: VerifyPatternViewModel,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    CardTitle("请绘制您的图案")
    CardSubTitle("")
    CommonPattern(viewModel, Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
  }
}

@Composable
private fun CommonPattern(
  viewModel: PatternViewModelWrapper,
  modifier: Modifier = Modifier,
) {
  val pattern = viewModel.pattern
  val points = pattern.points
  val path = pattern.path

  BoxWithConstraints(
    modifier = modifier
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset ->
            pattern.onDragStart(offset)
          },
          onDrag = { change, _ ->
            pattern.onDrag(change.position)
          },
          onDragEnd = {
            pattern.onDragEnd()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    viewModel.pattern.resize(min(maxWidth, maxHeight), LocalDensity.current.density)
    val grayColor = LocalColorful.current.Gray.current
    val blueColor = LocalColorful.current.Blue.current
    Canvas(modifier = Modifier.size(pattern.size)) {
      points.forEachIndexed { index, point ->
        val focusCircle = pattern.activePoints.contains(index)
        drawCircle(
          color = if (focusCircle) blueColor else grayColor,
          radius = if (focusCircle) 12f else 8f,
          center = point
        )
      }
      drawPath(
        path = path,
        color = blueColor,
        style = Stroke(width = 8f)
      )
    }

    // val patternList = remember { mutableListOf<Int>() }
    // val positionList =
    //   remember { mutableListOf(elements = Array(9) { Offset.Zero }) }
    // LazyVerticalGrid(GridCells.Fixed(3), Modifier.pointerInput(Unit) {
    //   detectDragGestures(onD)
    // }) {
    //   items(9) { index ->
    //     Box(Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
    //       Box(
    //         Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary)
    //           .clip(CircleShape)
    //           .onGloballyPositioned {
    //             positionList[index] = it.positionOnScreen()
    //           }
    //       )
    //       if (patternList.contains(index)) {
    //       }
    //     }
    //   }
    // }
  }
}

open class PatternViewModel() {
  val points = mutableStateListOf<Offset>()

  var path by mutableStateOf(Path())

  val activePoints = mutableStateListOf<Int>()

  var size by mutableStateOf(200.dp)

  fun resize(size: Dp = 200.dp, density: Float) {
    this.size = size
    val gridSize = 3
    val unitSize = size.value * density / gridSize
    points.clear()
    for (row in 0..<gridSize) {
      for (col in 0..<gridSize) {
        points.add(
          Offset(
            x = (col + 0.5f) * unitSize,
            y = (row + 0.5f) * unitSize
          )
        )
      }
    }

    resetPath()
  }

  open fun onDragStart(offset: Offset) {
    reset()
    addDragPointToPath(offset)
  }

  open fun onDrag(offset: Offset) {
    path = addDragPointToPath(offset)
  }

  open fun onDragEnd() {
    resetPath()
  }

  fun reset() {
    activePoints.clear()
    path.reset()
    activePath.reset()
  }

  private fun resetPath() {
    activePath.reset()
    activePoints.forEach { pointIndex ->
      activePath.addPoint(points[pointIndex])
    }
    path = activePath.copy()
  }

  private var activePath = Path()
  private fun addDragPointToPath(offset: Offset): Path {
    this.points.forEachIndexed { index, point ->
      if (point.isInProximity(offset) && !activePoints.contains(index)) {
        activePoints.add(index)
        activePath.addPoint(point)
        return activePath.copy()
      }
    }
    return activePath.copyAddPoint(offset)
  }

  private fun Path.copyAddPoint(point: Offset) = copy().apply { addPoint(point) }

  private fun Path.addPoint(point: Offset) = if (isEmpty) {
    moveTo(point.x, point.y)
  } else {
    lineTo(point.x, point.y)
  }


  private fun Offset.isInProximity(other: Offset, proximity: Float = 80f): Boolean {
    return (this - other).getDistance() <= proximity
  }
}

interface PatternViewModelWrapper {
  val pattern: PatternViewModel
}

class TipMessage(val tip: String, val isError: Boolean = false)
class VerifyPatternViewModel(override val task: CompletableDeferred<ByteArray>) :
  VerifyViewModelTask(KeychainMethod.Pattern), PatternViewModelWrapper {
  override val pattern = PatternViewModel()

  override fun keyTipCallback(keyTip: ByteArray?) {}

  override fun doFinish(): ByteArray {
    return jvmSha256(pattern.activePoints.joinToString(",").utf8Binary)
  }
}

class RegisterPatternViewModel(override val task: CompletableDeferred<ByteArray>) :
  RegisterViewModelTask(KeychainMethod.Pattern), PatternViewModelWrapper {

  var tipMessage by mutableStateOf<TipMessage?>(null)
  var firstPassword by mutableStateOf<String?>(null)
  var firstConfirmed by mutableStateOf(false)
    private set
  var secondPassword by mutableStateOf<String?>(null)
  var secondConfirmed by mutableStateOf(false)
    private set
  override val pattern = object : PatternViewModel() {
    //    result ->
//    if (result.size < 4) {
//  errorMessage = ""
//    }
    override fun onDragStart(offset: Offset) {
      tipMessage = TipMessage("完成后松开手指")
    }

    override fun onDragEnd() {
      super.onDragEnd()
      val result = activePoints.toList()
      if (firstConfirmed) {
        secondPassword = result.joinToString(",")
        if (secondPassword != firstPassword) {
          tipMessage = TipMessage("请重试", true)
          reset()
        } else {
          secondConfirmed = true
        }
      } else {
        if (result.size < 4) {
          tipMessage = TipMessage("至少连接4个点，请重试", true)
          reset()
        } else {
          firstPassword = result.joinToString(",")
        }
      }
    }
  }

  fun confirmFirst() {
    if (firstPassword != null) {
      firstConfirmed = true
      pattern.reset()
    }
  }

  fun restart() {
    tipMessage = TipMessage("绘制密码图案，请至少连接4个点")
    firstPassword = ""
    firstConfirmed = false
    secondPassword = ""
    secondConfirmed = false
    pattern.reset()
  }

  fun confirm() {
    if (secondConfirmed) {
      password = secondPassword ?: ""
      finish()
    }
  }

  private var password = ""

  init {
    restart()
  }

  /**
   * TODO 允许用户自定义背景图，从而强化密码
   * 对于背景图的使用要考虑到图片文件的不确定性，我们需要对起进行模糊、减色，从而确保用户即便传上来一个低分辨率的图片，我们也能编码成我们要的东西
   */
  override fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray {
    return jvmSha256(password.utf8Binary)
  }
}

