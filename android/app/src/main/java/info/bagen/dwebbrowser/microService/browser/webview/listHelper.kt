package info.bagen.dwebbrowser.microService.browser.webview

fun <E> List<E>.some(function: (it: E) -> Unit): E? {
    for (item in this) {
        function(item)
        return item
    }
    return null
}

fun <E> List<E>.every(function: (it: E) -> Unit): List<E>? {
    if (isEmpty()) {
        return null
    }
    for (item in this) {
        function(item)
    }
    return this
}

fun <E, R> List<E>.lets(function: (it: E) -> R): R? {
    for (item in this) {
        val res = function(item)
        if (res != null) {
            return res
        }
    }
    return null
}

fun <E> List<E>.until(function: (it: E) -> Boolean): Boolean? {
    for (item in this) {
        val res = function(item)
        if (res) {
            return true
        }
    }
    return null
}