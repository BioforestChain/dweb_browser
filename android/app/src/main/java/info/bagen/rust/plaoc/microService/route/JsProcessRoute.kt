package info.bagen.rust.plaoc.microService.route

import info.bagen.rust.plaoc.microService.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsProcessRoute() {
    get("/listen") {
        val port =  call.request.queryParameters["port"]
        if (port.isNullOrEmpty()) {
            return@get call.respond(
                DefaultErrorResponse(
                    statusCode = 403,
                    errorMessage = "not found request param port"
                )
            )
        }
        println("https.sys.dweb#listen:$port,${Origin(httpNMM.createListen(port))}")

        call.respondText(httpNMM.createListen(port))
//          return@get call.respond(Origin(httpNMM.createListen(port)))
    }
    get("/create-process") {
        val mainCode: String? =
            call.request.queryParameters["mainCode"] ?: call.request.queryParameters["main_code"]
        if (mainCode.isNullOrEmpty()) {
            return@get call.respondText(
                DefaultErrorResponse(
                    statusCode = 403,
                    errorMessage = "not found request param mainCode or main_code"
                ).toString()
            )
        }
        println("jsProcessRoute#create-process: $mainCode")
        val processId =  httpNMM.jsMicroModule.createProcess(mainCode)
        call.respondText(processId.toString())
    }
}
