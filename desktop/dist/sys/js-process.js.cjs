"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.hookFetch = void 0;
/// 这个文件是给所有的 js-process(web-worker) 用的，所以会重写全局的 fetch 函数，思路与 dns 模块一致
const hookFetch = () => {
    const native_fetch = globalThis.fetch;
    globalThis.fetch = function fetch(url, init) {
        const from_app = this;
        let _parsed_url;
        let _request_init = init;
        if (typeof url === "string") {
            _parsed_url = new URL(url);
        }
        else if (url instanceof Request) {
            _parsed_url = new URL(url.url);
            _request_init = url;
        }
        else if (url instanceof URL) {
            _parsed_url = url;
        }
        if (_parsed_url !== undefined) {
            const parsed_url = _parsed_url;
            const request_init = _request_init ?? {};
            /// 进入特殊的解析模式
            if (parsed_url.protocol === "file:" &&
                parsed_url.hostname.endsWith(".dweb")) {
                const mmid = parsed_url.hostname;
                /// 拦截到了，走自定义总线
                let from_app_ipcs = connects.get(from_app);
                if (from_app_ipcs === undefined) {
                    from_app_ipcs = new Map();
                    connects.set(from_app, from_app_ipcs);
                }
                let ipc_promise = from_app_ipcs.get(mmid);
                if (ipc_promise === undefined) {
                    /// 初始化互联
                    ipc_promise = (async () => {
                        const app = await app_mm.open(parsed_url.hostname);
                        let req_id = 0;
                        const allocReqId = () => req_id++;
                        const ipc = await app.connect(from_app);
                        const reqresMap = new Map();
                        /// 监听回调
                        ipc.onMessage((message) => {
                            if (message instanceof IpcResponse) {
                                const response_po = reqresMap.get(message.req_id);
                                if (response_po) {
                                    reqresMap.delete(message.req_id);
                                    response_po.resolve(message);
                                }
                            }
                        });
                        ipc.onClose(() => {
                            from_app_ipcs?.delete(mmid);
                        });
                        return {
                            ipc,
                            reqresMap,
                            allocReqId,
                        };
                    })();
                    from_app_ipcs.set(mmid, ipc_promise);
                }
                return (async () => {
                    const { ipc, reqresMap, allocReqId } = await ipc_promise;
                    let body = "";
                    const method = request_init.method ?? "GET";
                    /// 读取 body
                    if (method === "POST" || method === "PUT") {
                        let buffer;
                        if (request_init.body instanceof ReadableStream) {
                            const reader = request_init.body.getReader();
                            const chunks = [];
                            while (true) {
                                const item = await reader.read();
                                if (item.done) {
                                    break;
                                }
                                chunks.push(item.value);
                            }
                            buffer = Buffer.concat(chunks);
                        }
                        else if (request_init.body instanceof Blob) {
                            buffer = Buffer.from(await request_init.body.arrayBuffer());
                        }
                        else if (ArrayBuffer.isView(request_init.body)) {
                            buffer = Buffer.from(request_init.body.buffer, request_init.body.byteOffset, request_init.body.byteLength);
                        }
                        else if (request_init.body instanceof ArrayBuffer) {
                            buffer = Buffer.from(request_init.body);
                        }
                        else if (typeof request_init.body === "string") {
                            body = request_init.body;
                        }
                        else if (request_init.body) {
                            throw new Error(`unsupport body type: ${request_init.body.constructor.name}`);
                        }
                        if (buffer !== undefined) {
                            body = buffer.toString("base64");
                        }
                    }
                    /// 读取 headers
                    let headers = Object.create(null);
                    if (request_init.headers) {
                        let req_headers;
                        if (request_init.headers instanceof Array) {
                            req_headers = new Headers(request_init.headers);
                        }
                        else if (request_init.headers instanceof Headers) {
                            req_headers = request_init.headers;
                        }
                        else {
                            headers = request_init.headers;
                        }
                        if (req_headers !== undefined) {
                            req_headers.forEach((value, key) => {
                                headers[key] = value;
                            });
                        }
                    }
                    /// 注册回调
                    const req_id = allocReqId();
                    const response_po = new PromiseOut();
                    reqresMap.set(req_id, response_po);
                    /// 发送
                    ipc.postMessage(new IpcRequest(req_id, method, parsed_url.href, body, headers));
                    const ipc_response = await response_po.promise;
                    return new Response(ipc_response.body, {
                        headers: ipc_response.headers,
                        status: ipc_response.statusCode,
                    });
                })();
            }
        }
        return native_fetch(url, init);
    };
};
exports.hookFetch = hookFetch;
