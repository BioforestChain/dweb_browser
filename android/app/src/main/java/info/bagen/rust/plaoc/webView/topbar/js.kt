package info.bagen.rust.plaoc.webView.topbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import info.bagen.rust.plaoc.webView.icon.DWebIcon
import info.bagen.rust.plaoc.webView.jsutil.DataString
import info.bagen.rust.plaoc.webView.jsutil.DataString_From
import info.bagen.rust.plaoc.webView.jsutil.JsUtil
import info.bagen.rust.plaoc.webView.jsutil.toData
import info.bagen.rust.plaoc.webView.network.getColorHex
import info.bagen.rust.plaoc.webView.network.hexToIntColor


private const val TAG = "TopBarFFI"

class TopBarFFI(
    val state: TopBarState,
) {

    fun topBarNavigationBack(): Boolean {
        state.doBack()
        return true
    }

    fun getTopBarShow(): Boolean {
        return state.enabled.value
    }

    fun setTopBarShow(isEnabled: Boolean): Boolean {
        state.enabled.value = isEnabled
//        Log.i(TAG, "toggleEnabled:${state.enabled.value}")
        return state.enabled.value
    }

    fun getTopBarOverlay(): Boolean {
        return state.overlay.value ?: false
    }

    /**设置遮罩*/
    fun setTopBarOverlay(isOverlay: String): Boolean {
//      Log.i(TAG, "toggleOverlay:${state.overlay.value},${isOverlay}")
        state.overlay.value = isOverlay.toBoolean()
        return state.overlay.value ?: false
    }

    /**设置透明度*/
    fun setTopBarAlpha(alpha: String): Float? {
        state.alpha.value = alpha.toFloat()
        return state.alpha.value
    }

    fun getTopBarTitle(): String {
        return state.title.value ?: ""
    }

    fun hasTopBarTitle(): Boolean {
        return state.title.value != null
    }

    fun setTopBarTitle(str: String): Boolean {
        state.title.value = str
        return true
    }

    fun getTopBarHeight(): Float {
        return state.height.value
    }

    fun getTopBarActions(): DataString<List<TopBarAction>> {
        return DataString_From(state.actions)//.map { action -> toDataString(action) }
    }

    fun setTopBarActions(actionListJson: DataString<List<TopBarAction>>): Boolean {
        state.actions.clear()
//      Log.i(TAG,"actionListJson:${actionListJson}")
        val actionList = actionListJson.toData<List<TopBarAction>>(object :
            TypeToken<List<TopBarAction>>() {}.type)
        actionList.toCollection(state.actions)
//      actionList.forEach{
//        Log.i(TAG,"哈哈：${it}")
//      }
        return true
    }

    fun getTopBarBackgroundColor(): String {
        return getColorHex(state.backgroundColor.value.toArgb())
    }

    fun setTopBarBackgroundColor(hexColor: String): Boolean {
        val color = hexToIntColor(hexColor)
        state.backgroundColor.value = Color(color)
        return true
    }

    fun getTopBarForegroundColor(): String {
        return getColorHex(state.foregroundColor.value.toArgb())
    }

    fun setTopBarForegroundColor(hexColor: String): Boolean {
        val color = hexToIntColor(hexColor)
        state.foregroundColor.value = Color(color)
        return true
    }

    companion object {
        val _x = TopBarAction._gson
    }
}

data class TopBarAction(
    val icon: DWebIcon,
    val onClickCode: String,
    val disabled: Boolean,
) {

    companion object {
        operator fun invoke(
            icon: DWebIcon,
            onClickCode: String,
            disabled: Boolean? = null
        ) = TopBarAction(
            icon, onClickCode, disabled ?: false
        )

        val _gson = JsUtil.registerGsonDeserializer(
            TopBarAction::class.java, JsonDeserializer { json, typeOfT, context ->
                val jsonObject = json.asJsonObject
                TopBarAction(
                    context.deserialize(jsonObject["icon"], DWebIcon::class.java),
                    jsonObject["onClickCode"].asString,
                    jsonObject["disabled"]?.asBoolean,
                )
            }
        )
        private val _icon_gson = DWebIcon._gson
    }
}
