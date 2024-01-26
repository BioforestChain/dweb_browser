package org.dweb_browser.js_backend.http

import node.http.ServerResponse

fun ServerResponse<*>.notFound(){
    statusCode = 404.0
    statusMessage = "404 Not Found"
    appendHeader("Content-Type", "text/plain")
    end()
}