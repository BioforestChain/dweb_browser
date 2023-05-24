"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PortListener = void 0;
const stream_1 = require("stream");
const _ReqMatcher_js_1 = require("../../helper/$ReqMatcher.js");
const createSignal_js_1 = require("../../helper/createSignal.js");
const httpMethodCanOwnBody_js_1 = require("../../helper/httpMethodCanOwnBody.js");
const readableStreamHelper_js_1 = require("../../helper/readableStreamHelper.js");
const urlHelper_js_1 = require("../../helper/urlHelper.js");
const defaultErrorResponse_js_1 = require("./defaultErrorResponse.js");
/**
 * > 目前只允许端口独占，未来会开放共享监听以及对应的路由策略（比如允许开发WASM版本的路由策略）
 */
class PortListener {
    constructor(ipc, host, origin) {
        Object.defineProperty(this, "ipc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ipc
        });
        Object.defineProperty(this, "host", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: host
        });
        Object.defineProperty(this, "origin", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: origin
        });
        Object.defineProperty(this, "_routers", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Set()
        });
        Object.defineProperty(this, "_on_destroy_signal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
        /** 监听 destroy 时间 */
        Object.defineProperty(this, "onDestroy", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: this._on_destroy_signal.listen
        });
    }
    addRouter(router) {
        this._routers.add(router);
        return () => {
            this._routers.delete(router);
        };
    }
    /**
     * 判断是否有绑定的请求
     * @param pathname
     * @param method
     * @returns
     */
    _isBindMatchReq(pathname, method) {
        for (const bind of this._routers) {
            for (const pathMatcher of bind.routes) {
                if ((0, _ReqMatcher_js_1.$isMatchReq)(pathMatcher, pathname, method)) {
                    return { bind, pathMatcher };
                }
            }
        }
    }
    /**
     * 接收 nodejs-web 请求
     * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
     */
    async hookHttpRequest(req, res) {
        // if (res.closed) {
        //   throw new Error("http server response already closed");
        // }
        const { url = "/", method = "GET" } = req;
        const parsed_url = (0, urlHelper_js_1.parseUrl)(url, this.origin);
        // console.log('parsed_url: ', parsed_url.href)
        const hasMatch = this._isBindMatchReq(parsed_url.pathname, method);
        if (hasMatch === undefined) {
            (0, defaultErrorResponse_js_1.defaultErrorResponse)(req, res, 404, "no found");
            return;
        }
        /**
         * 要通过 ipc 传输过去的 req.body
         *
         * 这里采用按需传输给远端（server 端发来 pull 指令时），
         * 同时如果关闭连接，这类也会关闭 client 的 req 的流连接
         * 反之亦然，如果 req 主动关闭了流连接， 我们也会关闭这个 stream
         */
        let ipc_req_body_stream;
        /// 如果是存在body的协议，那么将之读取出来
        /// 参考文档 https://www.rfc-editor.org/rfc/rfc9110.html#name-method-definitions
        if (
        /// 理论上除了 GET/HEAD/OPTIONS 之外的method （比如 DELETE）是允许包含 BODY 的，但这类严格的对其进行限制，未来可以通过启动监听时的配置来解除限制
        (0, httpMethodCanOwnBody_js_1.httpMethodCanOwnBody)(method)
        // &&
        // /// HTTP/1.x 的规范：（我们自己的 file: 参考了该标准）
        // (this.protocol === "http:" || this.protocol === "file:")
        //   ? /// 请求和响应主体要么需要发送 Content-Length 标头，以便另一方知道它将接收多少数据
        //     +(req.headers["content-length"] || 0) > 0 ||
        //     // 要么更改消息格式以使用分块编码。使用分块编码，正文被分成多个部分，每个部分都有自己的内容长度
        //     req.headers["transfer-encoding"] /* ?.includes("chunked") */
        //   : true
        ) {
            /** req body 的转发管道，转发到 响应服务端 */
            const server_req_body_writter = new readableStreamHelper_js_1.ReadableStreamOut();
            ;
            (async () => {
                const client_req_body_reader = stream_1.Readable.toWeb(req).getReader();
                client_req_body_reader.closed.then(() => {
                    server_req_body_writter.controller.close();
                });
                /// 根据数据拉取的情况，从 req 中按需读取数据，这种按需读取会反压到 web 的请求层那边暂缓数据的发送
                for await (const _ of (0, readableStreamHelper_js_1.streamRead)((0, readableStreamHelper_js_1.streamFromCallback)(server_req_body_writter.onPull, client_req_body_reader.closed))) {
                    const item = await client_req_body_reader.read();
                    if (item.done) {
                        /// 客户端的传输一旦关闭，转发管道也要关闭
                        server_req_body_writter.controller.close();
                    }
                    else {
                        server_req_body_writter.controller.enqueue(item.value);
                    }
                }
            })();
            ipc_req_body_stream = server_req_body_writter.stream;
        }
        console.log(`分发消息 http://${req.headers.host}${url}`);
        // 分发消息
        const http_response_info = await hasMatch.bind.streamIpc.request(url, {
            method,
            body: ipc_req_body_stream,
            headers: req.headers,
        });
        console.log('消息返回了');
        /// 写回 res 对象
        res.statusCode = http_response_info.statusCode;
        http_response_info.headers.forEach((value, name) => {
            res.setHeader(name, value);
        });
        /// 204 和 304 不可以包含 body
        if (http_response_info.statusCode !== 204 &&
            http_response_info.statusCode !== 304) {
            // await (await http_response_info.stream()).pipeTo(res)
            const http_response_body = http_response_info.body.raw;
            if (http_response_body instanceof ReadableStream) {
                (0, readableStreamHelper_js_1.streamReadAll)(http_response_body, {
                    map(chunk) {
                        res.write(chunk);
                    },
                    complete() {
                        res.end();
                    },
                });
            }
            else {
                res.end(http_response_body);
                // res.end();/// nw.js 调用 http2 end 会导致 nw 崩溃死掉？
            }
        }
    }
    /** 销毁监听器内产生的引用 */
    destroy() {
        Array.from(this._routers).map(item => item.streamIpc.close()); // 停止 streamIpc 是否还有这个必要吗？？
        // 删除 Router 保存的IPC
        this._on_destroy_signal.emit();
    }
}
exports.PortListener = PortListener;
