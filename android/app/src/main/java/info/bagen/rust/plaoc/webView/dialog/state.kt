package info.bagen.rust.plaoc.webView.dialog


import android.webkit.JsPromptResult
import android.webkit.JsResult
import info.bagen.rust.plaoc.webView.jsutil.JsUtil


data class JsAlertConfiguration(
    val title: String,
    val content: String,
    val confirmText: String,
    val dismissOnBackPress: Boolean,
    val dismissOnClickOutside: Boolean,
) {
    companion object {
        operator fun invoke(
            title: String,
            content: String,
            confirmText: String,
            dismissOnBackPress: Boolean? = null,
            dismissOnClickOutside: Boolean? = null,
        ) = JsAlertConfiguration(
            title,
            content,
            confirmText,
            dismissOnBackPress ?: true,
            dismissOnClickOutside ?: false,
        )

        val _gson =
            JsUtil.registerGsonDeserializer(JsAlertConfiguration::class.java) { json, typeOfT, context ->
                val jsonObject = json.asJsonObject
                JsAlertConfiguration(
                    jsonObject["title"]?.asString ?: "",
                    jsonObject["content"]?.asString ?: "",
                    jsonObject["confirmText"]?.asString ?: "",
                    jsonObject["dismissOnBackPress"]?.asBoolean,
                    jsonObject["dismissOnClickOutside"]?.asBoolean,
                )
            }
    }

    lateinit var onConfirm: () -> Unit
    fun bindCallback(onConfirm: () -> Unit): JsAlertConfiguration {
        this.onConfirm = onConfirm
        return this
    }

    fun bindCallback(result: JsResult): JsAlertConfiguration {
        bindCallback { result.confirm() }
        return this
    }

}

data class JsPromptConfiguration(
    val title: String,
    val label: String,
    val defaultValue: String,
    val confirmText: String,
    val cancelText: String,
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = false
) {
    companion object {
        operator fun invoke(
            title: String,
            label: String,
            defaultValue: String,
            confirmText: String,
            cancelText: String,
            dismissOnBackPress: Boolean? = null,
            dismissOnClickOutside: Boolean? = null,
        ) = JsPromptConfiguration(
            title,
            label,
            defaultValue,
            confirmText,
            cancelText,
            dismissOnBackPress ?: true,
            dismissOnClickOutside ?: false,
        )

        val _gson =
            JsUtil.registerGsonDeserializer(JsPromptConfiguration::class.java) { json, typeOfT, context ->
                val jsonObject = json.asJsonObject
                JsPromptConfiguration(
                    jsonObject["title"]?.asString ?: "",
                    jsonObject["label"]?.asString ?: "",
                    jsonObject["defaultValue"]?.asString ?: "",
                    jsonObject["confirmText"]?.asString ?: "",
                    jsonObject["cancelText"]?.asString ?: "",
                    jsonObject["dismissOnBackPress"]?.asBoolean,
                    jsonObject["dismissOnClickOutside"]?.asBoolean,
                )
            }
    }

    lateinit var onSubmit: (String) -> Unit
    lateinit var onCancel: () -> Unit
    fun bindCallback(onConfirm: (String) -> Unit, onCancel: () -> Unit): JsPromptConfiguration {
        this.onSubmit = onConfirm
        this.onCancel = onCancel
        return this
    }

    fun bindCallback(result: JsPromptResult): JsPromptConfiguration {
        bindCallback({ result.confirm(it) }, { result.cancel() })
        return this
    }

}

data class JsConfirmConfiguration(
    val title: String,
    val message: String,
    val confirmText: String,
    val cancelText: String,
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = false,
) {
    companion object {
        operator fun invoke(
            title: String,
            message: String,
            confirmText: String,
            cancelText: String,
            dismissOnBackPress: Boolean? = null,
            dismissOnClickOutside: Boolean? = null,
        ) = JsConfirmConfiguration(
            title,
            message,
            confirmText,
            cancelText,
            dismissOnBackPress ?: true,
            dismissOnClickOutside ?: false,
        )

        val _gson =
            JsUtil.registerGsonDeserializer(JsConfirmConfiguration::class.java) { json, typeOfT, context ->
                val jsonObject = json.asJsonObject
                JsConfirmConfiguration(
                    jsonObject["title"]?.asString ?: "",
                    jsonObject["message"]?.asString ?: "",
                    jsonObject["confirmText"]?.asString ?: "",
                    jsonObject["cancelText"]?.asString ?: "",
                    jsonObject["dismissOnBackPress"]?.asBoolean,
                    jsonObject["dismissOnClickOutside"]?.asBoolean,
                )
            }
    }


    lateinit var onConfirm: () -> Unit
    lateinit var onCancel: () -> Unit
    fun bindCallback(onConfirm: () -> Unit, onCancel: () -> Unit): JsConfirmConfiguration {
        this.onConfirm = onConfirm
        this.onCancel = onCancel
        return this
    }

    fun bindCallback(result: JsResult): JsConfirmConfiguration {
        bindCallback({ result.confirm() }, { result.cancel() })
        return this
    }

}
