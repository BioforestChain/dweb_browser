package org.node

import org.khronos.webgl.Uint8Array
import org.nodejs.http.IncomingMessage
import org.nodejs.http.ServerResponse

open external class Readable

open external class Writable: org.node.WritableStream {
    override fun end(data: String)
    override fun end(data: String, cb: () -> Unit)
    override fun end(data: Uint8Array)
    override fun end(data: Uint8Array, cb: () -> Unit)
}

external interface WritableStream {
    fun end(data: String)
    fun end(data: String, cb: () -> Unit)
    fun end(data: Uint8Array)
    fun end(data: Uint8Array, cb: () -> Unit)
}

typealias RequestListener = (req: IncomingMessage, res: ServerResponse) -> Unit
