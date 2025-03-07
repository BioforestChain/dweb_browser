package org.dweb_browser.browser.desk.model

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.sys.window.core.constant.WindowMode

internal data class TaskbarAppModelState(
  var focus: Boolean = false, var visible: Boolean = false, var mode: WindowMode = WindowMode.FLOAT,
)

internal class TaskbarAppModel(
  val mmid: String,
  val icon: StrictImageResource?,
  running: Boolean,
  var isShowClose: Boolean = false,
  var state: TaskbarAppModelState = TaskbarAppModelState(),
) {
  val runningFlow = MutableStateFlow(running)
  val running get() = runningFlow.value

  val openingFlow = MutableStateFlow(false)
  val opening get() = openingFlow.value


  @Composable
  fun rememberAniProp() = remember {
    object : RememberObserver {
      private val rid = AnimationProp.ridAcc++
      val prop = AnimationProp.getOrCreate(mmid, rid)
      private fun free() {
        prop.unRef(rid)
      }

      override fun onAbandoned() {
        free()
      }

      override fun onForgotten() {
        free()
      }

      override fun onRemembered() {
      }
    }
  }.prop

  class AnimationProp(val mmid: MMID) {
    companion object {
      internal var ridAcc by atomic(0)
      private val all = SafeHashMap<String, AnimationProp>()
      fun getOrCreate(mmid: MMID, rid: Int) =
        all.getOrPut(mmid) { AnimationProp(mmid) }.also { prop -> prop.ref(rid) }

    }

    private val refs = SafeHashSet<Int>()
    fun ref(rid: Int) {
      refs += rid
      freeJob?.cancel()
      freeJob = null
    }

    private var freeJob: Job? = null
    fun unRef(rid: Int) {
      refs -= rid
      if (refs.isEmpty()) {
        freeJob ?: globalDefaultScope.launch {
          delay(1000)
          if (refs.isEmpty()) {
            all.remove(mmid)
          }
        }.also { freeJob = it }
      }
    }

    internal var offsetYDp by mutableStateOf(0.dp)
      private set
    private var targetOffsetY by mutableStateOf(0f)
    fun setOffsetY(offsetY: Float) {
      targetOffsetY = offsetY
//      offsetYDp = offsetY.dp
    }

    @Composable
    internal fun Effect() {
      offsetYDp = animateFloatAsState(targetOffsetY, taskbarAppAniSpec()).value.dp
    }
  }
}

private fun <T> taskbarAppAniSpec() =
  spring<T>(Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)