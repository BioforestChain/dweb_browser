package info.bagen.rust.plaoc.webView.systemui.js

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import info.bagen.rust.plaoc.webView.jsutil.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "VirtualKeyboardFFI"

class VirtualKeyboardFFI(
    private val overlay: MutableState<Boolean>,
    private val activity: ComponentActivity,
    private val view: View
) {

    companion object {

        private const val jsVirtualKeyboardNamespace = "virtualKeyboardSafeArea"
        private const val cssVirtualKeyboardNamespace = "--virtual-keyboard-safe-area"

        data class KeyboardSafeArea(
            val top: Float,
            val left: Float,
            val right: Float,
            val bottom: Float
        )

        private var KeyboardFullSafeArea = KeyboardSafeArea(0F, 0F, 0F, 0F)
        private var preKeyboardSafeArea = KeyboardFullSafeArea
        private var virtualKeyboardHeight = 0F

        /**
         * @TODO 在虚拟键盘弹出后，如果离开activity（比如通过文字选择器，选择input的文本然后跳到别的地方）
         *       再回来时，虚拟键盘相关的生命周期就没有正确在这里触发，那么虚拟键盘的值就没法正确下将回原位，
         *       所以需要通过activity的生命周期，为其进行强制性的重新注入这些变量
         */
        // 注入虚拟键盘相关的js/css变量
        fun injectVirtualKeyboardVars(
            jsUtil: JsUtil,
            density: Density,
            layoutDirection: LayoutDirection,
            isOverlayVirtualKeyboard: Boolean,
            imeInsets: WindowInsets,
            isOverlayNavigationBar: Boolean,
            navigationBarInsets: WindowInsets
        ) {
            val keyboardSafeArea = with(density) {
                virtualKeyboardHeight = imeInsets.getBottom(density).toDp().value

                if (!isOverlayVirtualKeyboard) {
                    return@with KeyboardFullSafeArea
                }
                // 如果是 NavigationBar 没有被覆盖，那么需要将其从总高度中减去
                val keyboardSafeInsets = if (!isOverlayNavigationBar) {
                    imeInsets.exclude(navigationBarInsets)
                } else {
                    imeInsets
                }

                val left = keyboardSafeInsets.getLeft(density, layoutDirection).toDp().value
                val top = keyboardSafeInsets.getTop(density).toDp().value
                val right = keyboardSafeInsets.getRight(density, layoutDirection).toDp().value
                val bottom = keyboardSafeInsets.getBottom(density).toDp().value


                KeyboardSafeArea(top, left, right, bottom)
            }
            if (keyboardSafeArea != preKeyboardSafeArea) {
                preKeyboardSafeArea = keyboardSafeArea
                Log.i(TAG, "InjectVirtualKeyboardVars:$keyboardSafeArea")

                GlobalScope.launch {
                    jsUtil.setJsValues(
                        jsVirtualKeyboardNamespace, mapOf(
                            "top" to JsValue(JsValueType.Number, keyboardSafeArea.top.toString()),
                            "left" to JsValue(
                                JsValueType.Number,
                                keyboardSafeArea.right.toString()
                            ),
                            "right" to JsValue(
                                JsValueType.Number,
                                keyboardSafeArea.right.toString()
                            ),
                            "bottom" to JsValue(
                                JsValueType.Number,
                                keyboardSafeArea.bottom.toString()
                            ),
                        )
                    )
                    jsUtil.setCssVars(
                        "html",
                        mapOf(
                            "$cssVirtualKeyboardNamespace-top" to "${keyboardSafeArea.top}px",
                            "$cssVirtualKeyboardNamespace-left" to "${keyboardSafeArea.left}px",
                            "$cssVirtualKeyboardNamespace-right" to "${keyboardSafeArea.right}px",
                            "$cssVirtualKeyboardNamespace-bottom" to "${keyboardSafeArea.bottom}px",
                        )
                    )

                }
            }

        }
    }

    /**获取安全区域*/
    fun getSafeArea(): DataString<KeyboardSafeArea> {
        return DataString_From(preKeyboardSafeArea)
    }

    fun getHeight(): Float {
        return virtualKeyboardHeight
    }

    fun getOverlay(): Boolean {
        return overlay.value
    }

    /**设置是否覆盖内容*/
    fun setOverlay(isOverlay: String): Boolean {
        overlay.value = isOverlay.toBoolean()
        Log.i(TAG, "toggleOverlay:${isOverlay}")
        return overlay.value
    }

    private val imm by lazy { activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    fun show(): Boolean {
        activity.runOnUiThread {
            if (view.requestFocus()) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        return true
    }

    fun hide(): Boolean {
        activity.runOnUiThread {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        return true
    }
}
