package info.bagen.rust.plaoc.webView.jsutil

import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import com.google.gson.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KProperty

class JsUtil(
    val activity: ComponentActivity,
    val evaluateJavascript: (code: String, callback: ValueCallback<String>) -> Unit
) {

    private suspend fun evaluate(code: String): String {
        return suspendCoroutine { cont ->
            activity.runOnUiThread {
                evaluateJavascript(code) { result ->
                    cont.resume(result)
                }
            }
        }
    }

    private var _curJob: Job? = null
    private var _queueJobsCode = ConcurrentHashMap<String, () -> String>()

    fun evalQueue(id: String? = null, codeGetter: () -> String) {
        if (_curJob != null) {
            _queueJobsCode[id ?: "__null__"] = codeGetter
        }
        _curJob = GlobalScope.launch {
            var curCode = codeGetter()
            eval@ while (true) {
                evaluate(curCode)
                if (_queueJobsCode.isNotEmpty()) {
                    curCode = _queueJobsCode.map {
                        _queueJobsCode.remove(it.key)// 在迭代的时候删除，而不是map完clear，这里是为了确保线程并行时逻辑正常
                        it.value()
                    }.joinToString(";")
                } else {
                    break@eval
                }
            }
            _curJob = null
        }
    }


    private val ss_var_name_def: Deferred<String> = GlobalScope.async {
        val ss_var_name = "__android_util_css__"
        evaluate(
            """
            (()=>{
                const ss_key = `$ss_var_name`;
                const styleSheet = globalThis[ss_key] || (()=>{
                   const sheet = new CSSStyleSheet();
                   Object.defineProperty(globalThis, ss_key, {
                      enumerable:false,
                      writable:false,
                      configurable:false,
                      value:sheet
                   });
                   document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];
                   return sheet
                })();
                return ss_key;
            })()
            """,
        )
        ss_var_name
    }

    private val adoptedStyleSheet: MutableMap</*selector*/String, CSSRule> by lazy {
        mutableMapOf()
    }


    inner class CSSRule(
        val ss_var_name: String,
        val rule_index: Int,
        val selector: String,
        val cssPropertieMap: MutableMap<String, String> = mutableMapOf()
    ) {
        fun setProperty(name: String, prop: String): Boolean {
            if (cssPropertieMap[name] == prop) {
                return false
            }
            cssPropertieMap[name] = prop
            resetRule()
            return true
        }

        private fun resetRule() {
            evalQueue(
                "ss-replace-$rule_index"
            ) {
                """
                 $ss_var_name.deleteRule($rule_index);
                 $ss_var_name.insertRule(`${toCssString()}`, $rule_index);
             """.trimIndent()
            }
        }

        fun setProperties(kvs: Map<String, String>): Boolean {
            var changed = false
            for (kv in kvs) {
                if (cssPropertieMap[kv.key] != kv.value) {
                    cssPropertieMap[kv.key] = kv.value
                    changed = true
                }
            }
            if (changed) {
                resetRule()
            }

            return changed
        }

        private fun toCssString(): String {
            return """$selector {
            ${
                cssPropertieMap.map {
                    "${it.key}: ${it.value}"
                }.joinToString(";\n")
            }
                }"""
        }

        fun getProperty(name: String): String? {
            return cssPropertieMap[name]
        }

        fun delProperty(name: String): Boolean {
            return (!cssPropertieMap.remove(name).isNullOrEmpty()).also { deleted ->
                if (deleted) {
                    resetRule()
                }
            }
        }
    }

    private val css_lock by lazy { Mutex() }

    private suspend fun getCssRule(selector: String): CSSRule {
        css_lock.lock()
        try {
            return adoptedStyleSheet.getOrElse(selector) {

                val ss_var_name = ss_var_name_def.await()
                val rule_index = evaluate(
                    """
                    $ss_var_name.insertRule(`$selector{}`)
                """.trimIndent()
                ).toInt()
                val cssRule = CSSRule(ss_var_name, rule_index, selector).also {
                    adoptedStyleSheet[selector] = it
                }

                return cssRule
            }
        } finally {
            css_lock.unlock()
        }
    }

    suspend fun setCssVar(selector: String, name: String, value: String): Boolean {
        return getCssRule(selector).setProperty(name, value)
    }

    suspend fun setCssVars(selector: String, map: Map<String, String>): Boolean {
        return getCssRule(selector).setProperties(map)
    }

    suspend fun getCssVar(selector: String, name: String): String? {
        return getCssRule(selector).getProperty(name)
    }

    suspend fun delCssVar(selector: String, name: String): Boolean {
        return getCssRule(selector).delProperty(name)
    }


    inner class JsNamespace(
        val js_var_name: String,
        val namespace: String,
        val jsPropertieMap: MutableMap<String, JsValue> = mutableMapOf()
    ) {
        fun setProperty(name: String, prop: JsValue): Boolean {
            if (jsPropertieMap[name] == prop) {
                return false
            }
            jsPropertieMap[name] = prop
            changeMap[name] = prop

            return true
        }

        private val changeMap = ConcurrentHashMap<String, JsValue?>()
        private fun effectChanges() {
            evalQueue(
                "js-replace-$namespace",
            ) {
                """{
                const namespace = $js_var_name[`$namespace`];
                ${
                    changeMap.map {
                        "namespace[`${it.key}`] = ${it.value?.jsCode ?: "undefined"};\n"
                    }.joinToString("")
                }
                }""".trimIndent()
            }
        }

        fun setProperties(kvs: Map<String, JsValue>): Boolean {
            var changed = false
            for (kv in kvs) {
                if (jsPropertieMap[kv.key] != kv.value) {
                    jsPropertieMap[kv.key] = kv.value
                    changeMap[kv.key] = kv.value
                    changed = true
                }
            }
            if (changed) {
                effectChanges()
            }

            return changed
        }

        fun getProperty(name: String): JsValue? {
            return jsPropertieMap[name]
        }

        fun delProperty(name: String): Boolean {
            return (jsPropertieMap.remove(name) != null).also { deleted ->
                if (deleted) {
                    changeMap[name] = null
                    effectChanges()
                }
            }

        }
    }

    private val jsNamespaceMap: MutableMap</*selector*/String, JsNamespace> by lazy {
        mutableMapOf()
    }
    private val js_var_name_def: Deferred<String> = GlobalScope.async {
        val js_var_name = "__android_util_js__"
        evaluate(
            """
            (()=>{
                const js_key = `$js_var_name`;
                const js = globalThis[js_key] || (()=>{
                   const js = {};
                   Object.defineProperty(globalThis, js_key, {
                      enumerable:false,
                      writable:false,
                      configurable:false,
                      value: js
                   });
                   return js
                })();
                return js_key;
            })()
            """,
        )
        js_var_name
    }
    private val js_lock by lazy { Mutex() }


    private suspend fun getJsNamespace(namespace: String): JsNamespace {
        try {
            js_lock.lock()

            return jsNamespaceMap.getOrElse(namespace) {
                val js_var_name = js_var_name_def.await()
                evaluate(
                    """$js_var_name[`${namespace}`] = {};void 0;""".trimIndent()
                )
                val jsNamespace = JsNamespace(js_var_name, namespace).also {
                    jsNamespaceMap[namespace] = it
                }
                return jsNamespace
            }
        } finally {
            js_lock.unlock()

        }
    }

    suspend fun setJsValue(namespace: String, name: String, value: JsValue) {
        getJsNamespace(namespace).setProperty(name, value)
    }

    suspend fun setJsValues(namespace: String, map: Map<String, JsValue>): Boolean {
        return getJsNamespace(namespace).setProperties(map)
    }

    suspend fun getJsValue(namespace: String, name: String): JsValue? {
        return getJsNamespace(namespace).getProperty(name)
    }

    suspend fun delJsValue(namespace: String, name: String): Boolean {
        return getJsNamespace(namespace).delProperty(name)
    }

    companion object {

        fun <T> resetAbleLazy(initializer: () -> T) = ResetAbleDelegate(initializer)

        class ResetAbleDelegate<T>(private val initializer: () -> T) {
            private val lazyRef: AtomicReference<Lazy<T>> = AtomicReference(
                lazy(
                    initializer
                )
            )

            operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return lazyRef.get().getValue(thisRef, property)
            }

            fun reset() {
                lazyRef.set(lazy(initializer))
            }
        }

        private val gsonBuilder = GsonBuilder()
        private val gsonDelegate = resetAbleLazy {
            gsonBuilder.create()
        }
        val gson by gsonDelegate

        fun <T> registerGsonDeserializer(type: Type, typeAdapter: JsonDeserializer<T>) {
            gsonBuilder.registerTypeAdapter(type, typeAdapter)
            gsonDelegate.reset()
        }
    }
}

data class JsValue(val type: JsValueType, val value: String) {
    fun isNullOrEmpty(): Boolean {
        return false
    }

    val jsCode: String by lazy {
        when (type) {
            JsValueType.String -> JsUtil.gson.toJson(value)
            JsValueType.Number -> value.toDoubleOrNull()?.let { value } ?: "NaN"
            JsValueType.Boolean -> if (value == "true") "true" else "false"
            JsValueType.RegExp -> "/$value/"
            JsValueType.RAW -> value
        }
    }
}

enum class JsValueType {
    String, Number, Boolean, RegExp, RAW,
}

typealias ColorInt = Int

fun ColorInt.toComposeColor() = Color(this)


typealias BoolInt = Int

fun BoolInt.toBoolean(elseDefault: () -> Boolean = { false }): Boolean {
    return when {
        this > 0 -> {
            true
        }
        this < 0 -> {
            false
        }
        else -> {
            elseDefault()
        }
    }
}

fun BoolInt.toBooleanOrNull(): Boolean? {
    return when {
        this > 0 -> {
            true
        }
        this < 0 -> {
            false
        }
        else -> {
            null
        }
    }
}


typealias DataString<T> = String

@Throws(JsonSyntaxException::class)
fun <T> DataString<T>.toData(classOfT: Class<T>): T {
    return JsUtil.gson.fromJson(this, classOfT)
}

fun <T> DataString<T>.toData(typeOfT: Type): T {
    val res: T = JsUtil.gson.fromJson(this, typeOfT)
    return res
}

fun <T> DataString_From(v: T): DataString<T> {
    return JsUtil.gson.toJson(v)
}

typealias CallbackString = String

fun CallbackString.callJs(callId: String, jsUtil: JsUtil, vararg args: String) {
    jsUtil.evalQueue(callId) { "$this(${args.joinToString(",")})" }
}
