@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS"
)

package npm.ws;

import node.buffer.Buffer
import node.events.EventEmitter
import node.http.ClientRequest
import node.http.ClientRequestArgs
import node.http.IncomingMessage
import node.http.OutgoingHttpHeaders
import node.stream.Duplex
import node.stream.DuplexOptions
import org.w3c.dom.url.URL
import tsstdlib.InstanceType
import tsstdlib.Set

typealias VerifyClientCallbackSync<Request> = (info: VerifyClientCallbackOptions<Request>) -> Boolean

typealias VerifyClientCallbackAsync<Request> = (info: VerifyClientCallbackOptions<Request>, callback: (res: Boolean, code: Number, message: String, headers: OutgoingHttpHeaders) -> Unit) -> Unit

external interface VerifyClientCallbackOptions<Request> {
  var origin: String
  var secure: Boolean
  var req: Request
}


@JsModule("ws")
open external class WebSocket : EventEmitter {
  constructor(address: Nothing?)
  constructor(address: String, options: ClientOptions = definedExternally)
  constructor(address: String)
  constructor(address: String, options: ClientRequestArgs = definedExternally)
  constructor(address: URL, options: ClientOptions = definedExternally)
  constructor(address: URL)
  constructor(address: URL, options: ClientRequestArgs = definedExternally)
  constructor(
    address: String,
    protocols: String = definedExternally,
    options: ClientOptions = definedExternally
  )

  constructor(address: String, protocols: String = definedExternally)
  constructor(
    address: String,
    protocols: String = definedExternally,
    options: ClientRequestArgs = definedExternally
  )

  constructor(
    address: String,
    protocols: Array<String> = definedExternally,
    options: ClientOptions = definedExternally
  )

  constructor(address: String, protocols: Array<String> = definedExternally)
  constructor(
    address: String,
    protocols: Array<String> = definedExternally,
    options: ClientRequestArgs = definedExternally
  )

  constructor(
    address: URL,
    protocols: String = definedExternally,
    options: ClientOptions = definedExternally
  )

  constructor(address: URL, protocols: String = definedExternally)
  constructor(
    address: URL,
    protocols: String = definedExternally,
    options: ClientRequestArgs = definedExternally
  )

  constructor(
    address: URL,
    protocols: Array<String> = definedExternally,
    options: ClientOptions = definedExternally
  )

  constructor(address: URL, protocols: Array<String> = definedExternally)
  constructor(
    address: URL,
    protocols: Array<String> = definedExternally,
    options: ClientRequestArgs = definedExternally
  )

  open var binaryType: String /* "nodebuffer" | "arraybuffer" | "fragments" */
  open var bufferedAmount: Number
  open var extensions: String
  open var isPaused: Boolean
  open var protocol: String
  open var readyState: Any
  open var url: String
  open var CONNECTING: Number /* 0 */
  open var OPEN: Number /* 1 */
  open var CLOSING: Number /* 2 */
  open var CLOSED: Number /* 3 */
  open var onopen: ((event: Event) -> Unit)?
  open var onerror: ((event: ErrorEvent) -> Unit)?
  open var onclose: ((event: CloseEvent) -> Unit)?
  open var onmessage: ((event: MessageEvent) -> Unit)?
  open fun close(code: Number = definedExternally, data: String = definedExternally)
  open fun close()
  open fun close(code: Number = definedExternally)
  open fun close(code: Number = definedExternally, data: Buffer = definedExternally)
  open fun ping(
    data: Any = definedExternally,
    mask: Boolean = definedExternally,
    cb: (err: Error) -> Unit = definedExternally
  )

  open fun pong(
    data: Any = definedExternally,
    mask: Boolean = definedExternally,
    cb: (err: Error) -> Unit = definedExternally
  )

  open fun send(
    data: Any /* String | Buffer | DataView | Number | ArrayBufferView | Uint8Array | ArrayBuffer | SharedArrayBuffer | Array<Any> | Array<Number> | `T$4` | `T$5` | `T$6` | `T$7` | `T$8` | Any */,
    cb: (err: Error) -> Unit = definedExternally
  )

  open fun send(data: Any /* String | Buffer | DataView | Number | ArrayBufferView | Uint8Array | ArrayBuffer | SharedArrayBuffer | Array<Any> | Array<Number> | `T$4` | `T$5` | `T$6` | `T$7` | `T$8` | Any */)
  open fun send(
    data: Any /* String | Buffer | DataView | Number | ArrayBufferView | Uint8Array | ArrayBuffer | SharedArrayBuffer | Array<Any> | Array<Number> | `T$4` | `T$5` | `T$6` | `T$7` | `T$8` | Any */,
    options: SendOptions,
    cb: (err: Error) -> Unit = definedExternally
  )

  open fun send(
    data: Any /* String | Buffer | DataView | Number | ArrayBufferView | Uint8Array | ArrayBuffer | SharedArrayBuffer | Array<Any> | Array<Number> | `T$4` | `T$5` | `T$6` | `T$7` | `T$8` | Any */,
    options: SendOptions
  )


  interface SendOptions {
    var mask: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var binary: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var compress: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var fin: Boolean?
      get() = definedExternally
      set(value) = definedExternally
  }

  open fun terminate()
  open fun pause()
  open fun resume()
  open fun addEventListener(
    method: String /* "message" */,
    cb: (event: MessageEvent) -> Unit,
    options: EventListenerOptions = definedExternally
  )

  open fun addEventListener(method: String /* "message" */, cb: (event: MessageEvent) -> Unit)
  open fun addEventListener(
    method: String /* "close" */,
    cb: (event: CloseEvent) -> Unit,
    options: EventListenerOptions = definedExternally
  )

  open fun addEventListener(method: String /* "close" */, cb: (event: CloseEvent) -> Unit)
  open fun addEventListener(
    method: String /* "error" */,
    cb: (event: ErrorEvent) -> Unit,
    options: EventListenerOptions = definedExternally
  )

  open fun addEventListener(method: String /* "error" */, cb: (event: ErrorEvent) -> Unit)
  open fun addEventListener(
    method: String /* "open" */,
    cb: (event: Event) -> Unit,
    options: EventListenerOptions = definedExternally
  )

  open fun addEventListener(method: String /* "open" */, cb: (event: Event) -> Unit)
  open fun removeEventListener(method: String /* "message" */, cb: (event: MessageEvent) -> Unit)
  open fun removeEventListener(method: String /* "close" */, cb: (event: CloseEvent) -> Unit)
  open fun removeEventListener(method: String /* "error" */, cb: (event: ErrorEvent) -> Unit)
  open fun removeEventListener(method: String /* "open" */, cb: (event: Event) -> Unit)
  open fun on(
    event: String /* "close" */,
    listener: (self: WebSocket, code: Number, reason: Buffer) -> Unit
  ): WebSocket /* this */

  open fun on(
    event: String /* "error" */,
    listener: (self: WebSocket, err: Error) -> Unit
  ): WebSocket /* this */

  open fun on(
    event: String /* "upgrade" */,
    listener: (self: WebSocket, request: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun on(
    event: String /* "message" */,
    listener: (self: WebSocket, data: Any /* Buffer | ArrayBuffer | Array<Buffer> */, isBinary: Boolean) -> Unit
  ): WebSocket /* this */

  open fun on(event: String /* "open" */, listener: (self: WebSocket) -> Unit): WebSocket /* this */
  open fun on(
    event: String /* "ping" | "pong" */,
    listener: (self: WebSocket, data: Buffer) -> Unit
  ): WebSocket /* this */

  open fun on(
    event: String /* "unexpected-response" */,
    listener: (self: WebSocket, request: ClientRequest, response: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun on(event: String, listener: (self: WebSocket, args: Any) -> Unit): WebSocket /* this */
  open fun on(event: Any, listener: (self: WebSocket, args: Any) -> Unit): WebSocket /* this */
  open fun once(
    event: String /* "close" */,
    listener: (self: WebSocket, code: Number, reason: Buffer) -> Unit
  ): WebSocket /* this */

  open fun once(
    event: String /* "error" */,
    listener: (self: WebSocket, err: Error) -> Unit
  ): WebSocket /* this */

  open fun once(
    event: String /* "upgrade" */,
    listener: (self: WebSocket, request: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun once(
    event: String /* "message" */,
    listener: (self: WebSocket, data: Any /* Buffer | ArrayBuffer | Array<Buffer> */, isBinary: Boolean) -> Unit
  ): WebSocket /* this */

  open fun once(
    event: String /* "open" */,
    listener: (self: WebSocket) -> Unit
  ): WebSocket /* this */

  open fun once(
    event: String /* "ping" | "pong" */,
    listener: (self: WebSocket, data: Buffer) -> Unit
  ): WebSocket /* this */

  open fun once(
    event: String /* "unexpected-response" */,
    listener: (self: WebSocket, request: ClientRequest, response: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun once(event: String, listener: (self: WebSocket, args: Any) -> Unit): WebSocket /* this */
  open fun once(event: Any, listener: (self: WebSocket, args: Any) -> Unit): WebSocket /* this */
  open fun off(
    event: String /* "close" */,
    listener: (self: WebSocket, code: Number, reason: Buffer) -> Unit
  ): WebSocket /* this */

  open fun off(
    event: String /* "error" */,
    listener: (self: WebSocket, err: Error) -> Unit
  ): WebSocket /* this */

  open fun off(
    event: String /* "upgrade" */,
    listener: (self: WebSocket, request: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun off(
    event: String /* "message" */,
    listener: (self: WebSocket, data: Any /* Buffer | ArrayBuffer | Array<Buffer> */, isBinary: Boolean) -> Unit
  ): WebSocket /* this */

  open fun off(
    event: String /* "open" */,
    listener: (self: WebSocket) -> Unit
  ): WebSocket /* this */

  open fun off(
    event: String /* "ping" | "pong" */,
    listener: (self: WebSocket, data: Buffer) -> Unit
  ): WebSocket /* this */

  open fun off(
    event: String /* "unexpected-response" */,
    listener: (self: WebSocket, request: ClientRequest, response: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun off(event: String, listener: (self: WebSocket, args: Any) -> Unit): WebSocket /* this */
  open fun off(event: Any, listener: (self: WebSocket, args: Any) -> Unit): WebSocket /* this */
  open fun addListener(
    event: String /* "close" */,
    listener: (code: Number, reason: Buffer) -> Unit
  ): WebSocket /* this */

  open fun addListener(
    event: String /* "error" */,
    listener: (err: Error) -> Unit
  ): WebSocket /* this */

  open fun addListener(
    event: String /* "upgrade" */,
    listener: (request: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun addListener(
    event: String /* "message" */,
    listener: (data: Any /* Buffer | ArrayBuffer | Array<Buffer> */, isBinary: Boolean) -> Unit
  ): WebSocket /* this */

  open fun addListener(event: String /* "open" */, listener: () -> Unit): WebSocket /* this */
  open fun addListener(
    event: String /* "ping" | "pong" */,
    listener: (data: Buffer) -> Unit
  ): WebSocket /* this */

  open fun addListener(
    event: String /* "unexpected-response" */,
    listener: (request: ClientRequest, response: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun addListener(event: String, listener: (args: Any) -> Unit): WebSocket /* this */
  open fun addListener(event: Any, listener: (args: Any) -> Unit): WebSocket /* this */
  open fun removeListener(
    event: String /* "close" */,
    listener: (code: Number, reason: Buffer) -> Unit
  ): WebSocket /* this */

  open fun removeListener(
    event: String /* "error" */,
    listener: (err: Error) -> Unit
  ): WebSocket /* this */

  open fun removeListener(
    event: String /* "upgrade" */,
    listener: (request: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun removeListener(
    event: String /* "message" */,
    listener: (data: Any /* Buffer | ArrayBuffer | Array<Buffer> */, isBinary: Boolean) -> Unit
  ): WebSocket /* this */

  open fun removeListener(event: String /* "open" */, listener: () -> Unit): WebSocket /* this */
  open fun removeListener(
    event: String /* "ping" | "pong" */,
    listener: (data: Buffer) -> Unit
  ): WebSocket /* this */

  open fun removeListener(
    event: String /* "unexpected-response" */,
    listener: (request: ClientRequest, response: IncomingMessage) -> Unit
  ): WebSocket /* this */

  open fun removeListener(event: String, listener: (args: Any) -> Unit): WebSocket /* this */
  open fun removeListener(event: Any, listener: (args: Any) -> Unit): WebSocket /* this */

  interface `T$2` {
    @nativeGetter
    operator fun get(key: String): String?

    @nativeSetter
    operator fun set(key: String, value: String)
  }

  interface `L$0` {
    @nativeInvoke
    operator fun invoke(servername: String, cert: String): Boolean

    @nativeInvoke
    operator fun invoke(servername: String, cert: Array<String>): Boolean

    @nativeInvoke
    operator fun invoke(servername: String, cert: Buffer): Boolean

    @nativeInvoke
    operator fun invoke(servername: String, cert: Array<Buffer>): Boolean
  }

  interface ClientOptions {
    var protocol: String?
      get() = definedExternally
      set(value) = definedExternally
    var followRedirects: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    val generateMask: ((mask: Buffer) -> Unit)?
    var handshakeTimeout: Number?
      get() = definedExternally
      set(value) = definedExternally
    var maxRedirects: Number?
      get() = definedExternally
      set(value) = definedExternally
    var perMessageDeflate: dynamic /* Boolean? | PerMessageDeflateOptions? */
      get() = definedExternally
      set(value) = definedExternally
    var localAddress: String?
      get() = definedExternally
      set(value) = definedExternally
    var protocolVersion: Number?
      get() = definedExternally
      set(value) = definedExternally
    var headers: `T$2`?
      get() = definedExternally
      set(value) = definedExternally
    var origin: String?
      get() = definedExternally
      set(value) = definedExternally
    var agent: Any?
      get() = definedExternally
      set(value) = definedExternally
    var host: String?
      get() = definedExternally
      set(value) = definedExternally
    var family: Number?
      get() = definedExternally
      set(value) = definedExternally
    val checkServerIdentity: `L$0`?
      get() = definedExternally
    var rejectUnauthorized: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var maxPayload: Number?
      get() = definedExternally
      set(value) = definedExternally
    var skipUTF8Validation: Boolean?
      get() = definedExternally
      set(value) = definedExternally
  }

  interface `T$3` {
    var flush: Number?
      get() = definedExternally
      set(value) = definedExternally
    var finishFlush: Number?
      get() = definedExternally
      set(value) = definedExternally
    var chunkSize: Number?
      get() = definedExternally
      set(value) = definedExternally
    var windowBits: Number?
      get() = definedExternally
      set(value) = definedExternally
    var level: Number?
      get() = definedExternally
      set(value) = definedExternally
    var memLevel: Number?
      get() = definedExternally
      set(value) = definedExternally
    var strategy: Number?
      get() = definedExternally
      set(value) = definedExternally
    var dictionary: dynamic /* Buffer? | Array<Buffer>? | DataView? */
      get() = definedExternally
      set(value) = definedExternally
    var info: Boolean?
      get() = definedExternally
      set(value) = definedExternally
  }

  interface PerMessageDeflateOptions {
    var serverNoContextTakeover: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var clientNoContextTakeover: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var serverMaxWindowBits: Number?
      get() = definedExternally
      set(value) = definedExternally
    var clientMaxWindowBits: Number?
      get() = definedExternally
      set(value) = definedExternally
    var zlibDeflateOptions: `T$3`?
      get() = definedExternally
      set(value) = definedExternally
    var zlibInflateOptions: Any?
      get() = definedExternally
      set(value) = definedExternally
    var threshold: Number?
      get() = definedExternally
      set(value) = definedExternally
    var concurrencyLimit: Number?
      get() = definedExternally
      set(value) = definedExternally
  }

  interface Event {
    var type: String
    var target: WebSocket
  }

  interface ErrorEvent {
    var error: Any
    var message: String
    var type: String
    var target: WebSocket
  }

  interface CloseEvent {
    var wasClean: Boolean
    var code: Number
    var reason: String
    var type: String
    var target: WebSocket
  }

  interface MessageEvent {
    var data: dynamic /* String | Buffer | ArrayBuffer | Array<Buffer> */
      get() = definedExternally
      set(value) = definedExternally
    var type: String
    var target: WebSocket
  }

  interface EventListenerOptions {
    var once: Boolean?
      get() = definedExternally
      set(value) = definedExternally
  }

  interface ServerOptions<U : Any, V : Any> {
    var host: String?
      get() = definedExternally
      set(value) = definedExternally
    var port: Number?
      get() = definedExternally
      set(value) = definedExternally
    var backlog: Number?
      get() = definedExternally
      set(value) = definedExternally
    var server: dynamic /* HTTPServer<V>? | HTTPSServer<V>? */
      get() = definedExternally
      set(value) = definedExternally
    var verifyClient: dynamic /* VerifyClientCallbackAsync<InstanceType<V>>? | VerifyClientCallbackSync<InstanceType<V>>? */
      get() = definedExternally
      set(value) = definedExternally
    var handleProtocols: ((protocols: Set<String>, request: InstanceType<V>) -> dynamic)?
      get() = definedExternally
      set(value) = definedExternally
    var path: String?
      get() = definedExternally
      set(value) = definedExternally
    var noServer: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var clientTracking: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var perMessageDeflate: dynamic /* Boolean? | PerMessageDeflateOptions? */
      get() = definedExternally
      set(value) = definedExternally
    var maxPayload: Number?
      get() = definedExternally
      set(value) = definedExternally
    var skipUTF8Validation: Boolean?
      get() = definedExternally
      set(value) = definedExternally
    var WebSocket: U?
      get() = definedExternally
      set(value) = definedExternally
  }

  interface AddressInfo {
    var address: String
    var family: String
    var port: Number
  }

  open class WebSocketServer<T : Any, U : Any>(
    options: ServerOptions<T, U> = definedExternally,
    callback: () -> Unit = definedExternally
  ) : EventEmitter {
    open var options: ServerOptions<T, U>
    open var path: String
    open var clients: Set<T>
    open fun address(): dynamic /* AddressInfo | String */
    open fun close(cb: (err: Error) -> Unit = definedExternally)
    open fun handleUpgrade(
      request: U,
      socket: Duplex,
      upgradeHead: Buffer,
      callback: (client: T, request: U) -> Unit
    )

    open fun shouldHandle(request: U): dynamic /* Boolean | Promise<Boolean> */
    open fun on(
      event: String /* "connection" */,
      cb: (self: WebSocketServer__1<T>, socket: T, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun on(
      event: String /* "error" */,
      cb: (self: WebSocketServer__1<T>, error: Error) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun on(
      event: String /* "headers" */,
      cb: (self: WebSocketServer__1<T>, headers: Array<String>, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun on(
      event: String /* "close" | "listening" */,
      cb: (self: WebSocketServer__1<T>) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun on(
      event: String,
      listener: (self: WebSocketServer__1<T>, args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun on(
      event: Any,
      listener: (self: WebSocketServer__1<T>, args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun once(
      event: String /* "connection" */,
      cb: (self: WebSocketServer__1<T>, socket: T, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun once(
      event: String /* "error" */,
      cb: (self: WebSocketServer__1<T>, error: Error) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun once(
      event: String /* "headers" */,
      cb: (self: WebSocketServer__1<T>, headers: Array<String>, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun once(
      event: String /* "close" | "listening" */,
      cb: (self: WebSocketServer__1<T>) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun once(
      event: String,
      listener: (self: WebSocketServer__1<T>, args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun once(
      event: Any,
      listener: (self: WebSocketServer__1<T>, args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun off(
      event: String /* "connection" */,
      cb: (self: WebSocketServer__1<T>, socket: T, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun off(
      event: String /* "error" */,
      cb: (self: WebSocketServer__1<T>, error: Error) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun off(
      event: String /* "headers" */,
      cb: (self: WebSocketServer__1<T>, headers: Array<String>, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun off(
      event: String /* "close" | "listening" */,
      cb: (self: WebSocketServer__1<T>) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun off(
      event: String,
      listener: (self: WebSocketServer__1<T>, args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun off(
      event: Any,
      listener: (self: WebSocketServer__1<T>, args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun addListener(
      event: String /* "connection" */,
      cb: (client: T, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun addListener(
      event: String /* "error" */,
      cb: (err: Error) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun addListener(
      event: String /* "headers" */,
      cb: (headers: Array<String>, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun addListener(
      event: String /* "close" | "listening" */,
      cb: () -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun addListener(
      event: String,
      listener: (args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun addListener(
      event: Any,
      listener: (args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun removeListener(
      event: String /* "connection" */,
      cb: (client: T, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun removeListener(
      event: String /* "error" */,
      cb: (err: Error) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun removeListener(
      event: String /* "headers" */,
      cb: (headers: Array<String>, request: U) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun removeListener(
      event: String /* "close" | "listening" */,
      cb: () -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun removeListener(
      event: String,
      listener: (args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */

    open fun removeListener(
      event: Any,
      listener: (args: Any) -> Unit
    ): WebSocketServer<T, U> /* this */
  }

  open class WebSocketServer__1<T : Any> : WebSocketServer<T, Any>
  open class WebSocketServer__0 : WebSocketServer<Any, Any>
  interface WebSocket : WebSocketAlias

  companion object {
    var CONNECTING: Number /* 0 */
    var OPEN: Number /* 1 */
    var CLOSING: Number /* 2 */
    var CLOSED: Number /* 3 */
    fun createWebSocketStream(
      websocket: WebSocket,
      options: DuplexOptions = definedExternally
    ): Duplex
  }
}

@Suppress("EXTERNAL_DELEGATION", "NESTED_CLASS_IN_EXTERNAL_INTERFACE")
external interface WebSocketAlias : WebSocket {
}