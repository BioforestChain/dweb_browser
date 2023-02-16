package info.bagen.rust.plaoc.microService.route

import info.bagen.rust.plaoc.microService.*
import info.bagen.rust.plaoc.microService.network.DefaultErrorResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsProcessRoute() {
    get("/listen") {
        val port =  call.request.queryParameters["port"]
        println("https.sys.dweb#listenxxxxx")
        if (port.isNullOrEmpty()) {
            return@get call.respond(DefaultErrorResponse(
                statusCode = 403,
                errorMessage = "not found request param port"
            ))
        }
//        println("https.sys.dweb#listen:$port,${Origin(global_dns.httpNMM.createListen(port))}")
//
//        call.respondText(global_dns.httpNMM.createListen(port))
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
        val processId =  global_dns.jsMicroModule.createProcess(mainCode)
        call.respondText(processId.toString())
    }
}
