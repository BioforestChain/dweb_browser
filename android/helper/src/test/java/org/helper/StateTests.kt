package org.helper

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.State
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class StateTests {

  @Test
  fun simpleTest() = runBlocking {
    val state1 = State(1)
    assertEquals(1, state1.get())
    state1.set(2)
    assertEquals(2, state1.get())
  }

  @Test
  fun baseTest() = runBlocking {

    val state1 = State(1)
    val state2 = State(1)
    val state3 = State {
      state1.get() + state2.get()
    }

    assertEquals(2, state3.get())

    state3.onChange { value ->
      println("state3: $value")
    }

    state2.set(2)
    println("state2: ${state2.get()}")
    assertEquals(3, state3.get())
    assertEquals(1, state1.refs.size)
    assertEquals(1, state2.refs.size)
    assertEquals(2, state3.deps.size)

    delay(100)
  }

  @Test
  fun complexTest() = runBlocking {

    val state1 = State(false)
    val state2 = State(2)
    val state3 = State(1)

    val state4 = State(
      if (state1.get()) {
        state2.get()
      } else {
        state3.get()
      }
    )

    assertEquals(1, state4.get())

    state4.onChange { value ->
      println("state4: $value")
    }

    state1.set(true)

    assertEquals(2, state4.get())

    delay(100)
  }

  @Test
  fun onChangeTest() = runBlocking {

    val visible = State(false)
    val overlay = State(false)

    val statusBarState = State(StatusBar(visible = visible.get(), overlay = overlay.get()))

    statusBarState.onChange { value ->
      println("onChange visible value: ${value.first.visible}")
      println("onChange visible oldValue: ${value.second?.visible}")
      println("onChange overlay value: ${value.first.overlay}")
      println("onChange overlay oldValue: ${value.second?.overlay}")
    }

    visible.set(true)

    val newStatusBar = statusBarState.get()

    println("visible: ${newStatusBar.visible}, overlay: ${newStatusBar.overlay}")

  }

  // 测试update仅适用于引用类型
  @Test
  fun updateRefTypeTest() = runBlocking {

    val statusBarState = State(StatusBar(false, false))

    statusBarState.update {
      it?.visible = true
      return@update true
    }

    assert(statusBarState.get().visible)

  }

  // 测试update不适用于值类型
  @Test
  fun updateValueTypeTest() = runBlocking {

    val navigationBarState = State(NavigationBar(false, false))

    navigationBarState.update {
      it?.visible = true
      return@update true
    }

    assert(navigationBarState.get().visible)

  }

  data class StatusBar(var visible: Boolean, var overlay: Boolean)

  data class NavigationBar(var visible: Boolean, var overlay: Boolean)
}

// 其他测试函数