package org.dweb_browser.core.http.router

import io.ktor.http.HttpStatusCode
import org.dweb_browser.helper.Debugger

val debugRoute = Debugger("route")

class ResponseException(
  val code: HttpStatusCode = HttpStatusCode.InternalServerError,
  override val message: String = code.description,
  cause: Throwable? = null,
) : Exception(message, cause)