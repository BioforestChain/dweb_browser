import type { StatusbarNativeUiNMM,  } from "./status-bar.main.cjs"
import type { $Schema1ToType } from "../../../helper/types.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs"
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs"
import type { $Schema2 } from "../../../helper/types.cjs" 
import type {  $RequestCommonHanlderSchema } from "../../../core/micro-module.native.cjs"
import { IpcResponse } from "../../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../../core/ipc/IpcHeaders.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs"
import { converRGBAToHexa } from "../helper.cjs"

export class CommonMesasgeRoutes{
    routes: $RequestCommonHanlderSchema<
        any ,
        $Schema2
    >[] = []

    constructor(
       readonly _nmm: StatusbarNativeUiNMM
    ){
        this
        .setBackground()
        .setVisible()
        .setOverlay()
        .setStyle()
        .getState()
    }

    /**
     * 设置背景色
     * @returns 
     */
    setBackground = () => {
        this.routes.push({
            pathname: "/operation/set_background_color",
            method: "GET",
            matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
            input: {
                app_url: "string",
                color : "object"
            },
            output: "boolean",
            handler: async (
                args: $Schema1ToType<{
                    app_url: "string";
                    color: "object";
                }>,
                ipc: Ipc,
                request: IpcRequest
            ) => {
                    if(args.app_url === null) {
                        return responseNoAppUrl(request.req_id, ipc)
                    }
                    return this._nmm._statusbarPluginRequestAdd(
                        args.app_url,
                        request,
                        (id: string) => {
                            this.sendMessage({
                                action: "operation",
                                operationName: "setBackgroundColor",
                                value: converRGBAToHexa(
                                    Reflect.get(args.color, 'red'),
                                    Reflect.get(args.color, 'green'),
                                    Reflect.get(args.color, 'blue'),
                                    Reflect.get(args.color, 'alpha'),
                                ),
                                from: args.app_url,
                                id: id
                            })
                        }
                    )
                }
            }
        )
        return this;
    }

    /**
     * 设置是否可见
     * @returns 
     */
    setVisible = () => {
        this.routes.push({
            pathname: "/operation/set_visible",
            method: "GET",
            matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
            input: {
                app_url: "string",
                visible: "boolean"
            },
            output: "boolean",
            handler: async (
                args: $Schema1ToType<{
                "app_url": "string";
                "visible": "boolean";
                }>, 
                ipc: Ipc,
                request: IpcRequest
            ) => {
                if(args.app_url === null) {
                    return responseNoAppUrl(request.req_id, ipc)
                }
            
                return this._nmm._statusbarPluginRequestAdd(
                    args.app_url,
                    request,
                    (id: string) => {
                        this.sendMessage({
                            action: "operation",
                            operationName: "set_visible",
                            value: args.visible,
                            from: args.app_url,
                            id: id
                        })
                    }
                )
            },
        })
        return this;
    }

    /**
     * 设置是否覆盖
     * @returns 
     */
    setOverlay = () => {
        this.routes.push({
            pathname: "/operation/set_overlay",
            method: "GET",
            matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
            input: {
                app_url: "string",
                visible: "boolean"
            },
            output: "boolean",
            handler: async (
                args: $Schema1ToType<{
                    app_url: "string";
                    overlay: "boolean"
                }>, 
                ipc: Ipc,
                request: IpcRequest
            ) => {
                if(args.app_url === null) {
                    return responseNoAppUrl(request.req_id, ipc)
                }
            
                return this._nmm._statusbarPluginRequestAdd(
                    args.app_url,
                    request,
                    (id: string) => {
                        this.sendMessage({
                            action: "operation",
                            operationName: "set_overlay",
                            value: args.overlay,
                            from: args.app_url,
                            id: id
                          })
                    }
                )
            },
        })
        return this;
    }

    /**
     * 设置样式
     * @returns 
     */
    setStyle = () => {
        this.routes.push({
            pathname: "/operation/set_style",
            method: "GET",
            matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
            input: {
                app_url: "string",
                style: "string"
            },
            output: "boolean",
            handler: async (
                args: $Schema1ToType<{
                    app_url: "string";
                    style: "string"
                }>, 
                ipc: Ipc,
                request: IpcRequest
            ) => {
                if(args.app_url === null) {
                    return responseNoAppUrl(request.req_id, ipc)
                }
                
                return this._nmm._statusbarPluginRequestAdd(
                    args.app_url,
                    request,
                    (id: string) => {
                        this.sendMessage({
                            action: "operation",
                            operationName: "set_style",
                            value: args.style,
                            from: args.app_url,
                            id: id
                          })
                    }
                )
            },
        })
        return this;
    }

    /**
     * 获取样式
     * @returns 
     */
    getState = () => {
        this.routes.push({
            pathname: "/operation/get_state",
            method: "GET",
            matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
            input: {
                app_url: "string",
            },
            output: "boolean",
            handler: async (
                args: $Schema1ToType<{
                    app_url: "string";
                }>, 
                ipc: Ipc,
                request: IpcRequest
            ) => {
                if(args.app_url === null) {
                    return responseNoAppUrl(request.req_id, ipc)
                }
            
                return this._nmm._statusbarPluginRequestAdd(
                    args.app_url,
                    request,
                    (id: string) => {
                        this.sendMessage({
                            action: "operation",
                            operationName: "get_state",
                            value: "",
                            from: args.app_url,
                            id: id
                        })
                    }
                )
            },
        })
        return this;
    }

    /**
     * 发数据发送给 status-bar UI
     * @param data 
     * @returns 
     */
    sendMessage = (data: Object) => {
        if(this._nmm.httpIpc === undefined) return;
        this._nmm.httpIpc
        .postMessage(
            IpcEvent
            .fromText(
                "http.sys.dweb", 
                JSON.stringify(data)
            )
        )
    }
}


/**
 * 创建一个 缺少 app_url 的 IpcResponse
 * @param req_id 
 * @param ipc 
 * @returns 
 */
async function responseNoAppUrl(
    req_id: number, 
    ipc: Ipc   
){
    /**已经测试走过了 */
    return IpcResponse.fromText(
        req_id,
        400,
        new IpcHeaders({
        "Content-type": "text/plain",
        }),
        "缺少 app_url 查询参数",
        ipc
    );
}