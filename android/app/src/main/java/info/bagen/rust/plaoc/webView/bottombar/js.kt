package info.bagen.rust.plaoc.webView.bottombar


import android.util.Log
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import info.bagen.rust.plaoc.webView.icon.DWebIcon
import info.bagen.rust.plaoc.webView.jsutil.*
import info.bagen.rust.plaoc.webView.network.getColorHex
import info.bagen.rust.plaoc.webView.network.hexToIntColor


private const val TAG = "BottomBarFFI"

class BottomBarFFI(
    private val state: BottomBarState,
) {
    fun getEnabled() = state.isEnabled

    // 控制是否隐藏bottom bar, 此方法如果不传不会调用，一传肯定是true，也就是不显示bottom bar
    fun setEnabled(isEnabledBool: String): Boolean {
        state.enabled.value = !isEnabledBool.toBoolean()
        return getEnabled()
    }

    fun getOverlay(): Boolean {
        return state.overlay.value ?: false
    }

    // 控制是开启bottom bar 遮罩。
    fun setOverlay(isOverlay: String): Boolean {
        state.overlay.value = isOverlay.toBoolean()
        return state.overlay.value ?: false
    }

    fun setAlpha(alpha: String): Float? {
        state.alpha.value = alpha.toFloat()
        return state.alpha.value
    }

    fun getHeight(): Float {
        println("state.height.value${state.height.value}")
        return state.height.value ?: 0F
    }

    fun setHeight(heightDp: String): String {
        try {
            state.height.value = heightDp.toFloat()
        } catch (e: Exception) {
            println(e.message)
            return "setHeight:${e.message}"
        }
        return heightDp
    }

    fun getActions(): DataString<List<BottomBarAction>> {
        return DataString_From(state.actions)//.map { action -> toDataString(action) }
    }

    fun setActions(actionListJson: DataString<List<BottomBarAction>>): Boolean {
        state.actions.clear()
        Log.i(TAG, "actionListJson:${actionListJson}")
        val actionList = actionListJson.toData<List<BottomBarAction>>(object :
            TypeToken<List<BottomBarAction>>() {}.type)
        actionList.toCollection(state.actions)
        return true
//      actionList.forEach{
//        Log.i(TAG, "actionList:${it.colors}")
//      }
    }

    fun getBackgroundColor(): String {
        return getColorHex(state.backgroundColor.value.toArgb())
    }

    fun setBackgroundColor(color: String): Boolean {
        state.backgroundColor.value = Color(hexToIntColor(color))
        return true
    }

    fun getForegroundColor(): String {
        return getColorHex(state.foregroundColor.value.toArgb())
    }

    fun setForegroundColor(color: String): Boolean {
        state.foregroundColor.value = Color(hexToIntColor(color))
        return true
    }

    companion object {
        private val _x = BottomBarAction._gson
    }
}


data class BottomBarAction(
    val icon: DWebIcon,
    val onClickCode: String,
    val label: String,
    val alwaysShowLabel: Boolean,
    val selected: Boolean,
    val selectable: Boolean,
    val disabled: Boolean,
    val colors: Colors?
) {

    data class Colors(
        val indicatorColor: String?,
        val iconColor: String?,
        val iconColorSelected: String?,
        val textColor: String?,
        val textColorSelected: String?
    ) {
        data class ColorState(override val value: Color) : State<Color>

        @Composable
        fun toNavigationBarItemColors(): NavigationBarItemColors {
            val defaultColors = NavigationBarItemDefaults.colors()
            // indicatorColor 无法控制透明度
            val indicatorColor = indicatorColor?.let { hexToIntColor(it).toComposeColor() }
                ?: defaultColors.indicatorColor
            val iconColor =
                ColorState(iconColor?.let { hexToIntColor(it).toComposeColor() }
                    ?: defaultColors.iconColor(false).value)
            val iconColorSelected = ColorState(
                iconColorSelected?.let { hexToIntColor(it).toComposeColor() }
                    ?: defaultColors.indicatorColor
            )
            val textColor =
                ColorState(textColor?.let { hexToIntColor(it).toComposeColor() }
                    ?: defaultColors.textColor(false).value)
            val textColorSelected = ColorState(
                textColorSelected?.let { hexToIntColor(it).toComposeColor() }
                    ?: defaultColors.indicatorColor
            )

            val colors = @Stable object : NavigationBarItemColors {
                override val indicatorColor: Color
                    @Composable get() = indicatorColor

                /**
                 * 表示该项的图标颜色，取决于它是否被[选中]。
                 *
                 * @param selected 项目是否被选中
                 */
                @Composable
                override fun iconColor(selected: Boolean) = if (selected) {
                    iconColorSelected
                } else {
                    iconColor
                }

                /**
                 * 表示该项的文本颜色，取决于它是否被[选中]。
                 *
                 * @param selected 项目是否被选中
                 */
                @Composable
                override fun textColor(selected: Boolean) = if (selected) {
                    textColorSelected
                } else {
                    textColor
                }
            }
            return colors
        }
    }

    companion object {
        operator fun invoke(
            icon: DWebIcon,
            onClickCode: String,
            label: String? = null,
            alwaysShowLabel: Boolean = true,
            selected: Boolean? = null,
            selectable: Boolean? = null,
            disabled: Boolean? = null,
            colors: Colors? = null
        ) = BottomBarAction(
            icon,
            onClickCode,
            label ?: "",
            alwaysShowLabel,
            selected ?: false,
            selectable ?: true,
            disabled ?: false,
            colors,
        )

        val _gson = JsUtil.registerGsonDeserializer(
            BottomBarAction::class.java, JsonDeserializer { json, typeOfT, context ->
                val jsonObject = json.asJsonObject
                BottomBarAction(
                    context.deserialize(jsonObject["icon"], DWebIcon::class.java),
                    jsonObject["onClickCode"].asString,
                    jsonObject["label"]?.asString,
                    jsonObject["alwaysShowLabel"].asBoolean,
                    jsonObject["selected"]?.asBoolean,
                    jsonObject["selectable"]?.asBoolean,
                    jsonObject["disabled"]?.asBoolean,
                    jsonObject["colors"]?.let {
                        context.deserialize(jsonObject["colors"], Colors::class.java)
                    }
                )
            }
        )
        private val _icon_gson = DWebIcon._gson
    }
}
