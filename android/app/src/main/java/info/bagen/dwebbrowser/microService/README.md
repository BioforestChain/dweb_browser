1. 注册表服务（根域名风格）
    ```ts
    class DwebDNS {
        map: Map<domain, MicroModule>
        query(domain:string):MicroModule?
    }
    ```
1. 通用通讯标准
    ```ts
    class IpcRequest{ 
        method:string
        url:string
        body:string
        headers:Map<string,string>
        onResponse = (response: IpcResponse)=>{}
     }
    class IpcResponse{ 
        request:IpcRequest
        statusCode:number
        body:string
    }
    class Ipc {
        postMessage (request: IpcRequest)
        onMessage = (request: IpcRequest)=>{}
    }
    ```
1. 微组件抽象类
    ```ts
    abstract class MicroModule{
        mmid:string
        abstract bootstrap()
        abstract ipc: Ipc
    }
    ```
1. 原生的微组件
    ```ts
    class NativeMicroModule extends MicroModule{
        mmid: `${string}.${'sys'|'std'}.dweb`
    }
    ```
    1. 启动组件
        ```ts
        class BootNMM extends NativeMicroModule{
            mmid: 'boot.sys.dweb'
            bootstrap() {
                // (new MicroModule.from('desktop.sys.dweb') as JsMicroModule).boostrap()
                for(const mmid of registeredMmids){
                    (new MicroModule.from(mmid)).boostrap()
                }
            }
            $Routers:{
                '/register': IO<mmid, boolean>
                '/unregister': IO<mmid, boolean>
            }
        }
        ```
    1. GUI组件
        ```ts
        class MultiWebViewNMM extends NativeMicroModule {
            mmid: 'mwebview.sys.dweb'
            bootstrap(args) {
                this.viewTree = new ViewTree();
            }
            $Routers: {
                '/open': IO<WindowOptions, number> & (ctx, args: WindowOptions) => {
                    const webviewNode = viewTree.createNode(Webview, args)
                    viewTree.appendTo(ctx.processId, webviewNode)
                    return webviewNode.id
                }
                '/evalJavascript/(:webview_id)': IO<code, json>
                '/listen/(:webview_id)': IO<void, {request_id}[]>
                '/request/(:request_id)': IO<void, json>
                '/response': IO<Response, void>
            }
        }

        const webview_id = await fetch('file://mwebview.sys.dweb/open').number();
        for await (const line of fetch(`file://mwebview.sys.dweb/listen/${webview_id}`).stream('jsonlines')) {
            const request = IpcRequest.from(await fetch(`file://mwebview.sys.dweb/request/${line.request_id}`).json());

            const response = IpcResponse(request, {...data});
            await fetch(`file://mwebview.sys.dweb/response`, { body:response });
        }

        import { MWebView } from '@bfex/mwebview';
        const webview = await MWebView.create();
        webview.onRequest((event)=>{
            event.responseWith()
        })
        ```
1. 可动态加载的微组件
    ```ts
    class JsMicroModule extends MicroModule{
        // 该程序的来源
        origin: `https://${string}`
        mmid: `${string}.${'bfs'|string}.dweb`
        bootstrap(args){
            /// 我们隐匿地启动单例webview视图，用它来动态创建 WebWorker，来实现 JavascriptContext 的功能
            const ctx = JavascriptContext.create(args.processId);
            ctx.evalJavascript(boostrap_code);// 为这个上下文安装启动代码
            ctx.evalJavascript(args.main_js);// 开始执行开发者自己的代码
        }
        ipc = new JsIpc()
    }
    class JsIpc {
        constructor(jsCtx) {
            const [port1,port2] = jsCtx.createMessageChannel();
            jsCtx.postMessage(port2)

            this.port1 = port1;
            port1.onMessage = (message) => this.onMessage(IpcRequest.from(message))
        }
        postMessage (request: IpcRequest) {
            this.port1.postMessage(request.stringify())
        }
        onMessage = (request: IpcRequest)=>{}
    }
    ```

    ```ts
    class WasnMicroModule extends MicroModule{
        bootstrap(args){
            /// 我们隐匿地启动单例webview视图，用它来动态创建 WebWorker，来实现 JavascriptContext 的功能
            const ctx = WasmerContext.create(args.processId, system_abi = {ipc_send});
            ctx.run(args.main_wasm);// 开始执行开发者自己的代码
        }
    }
    ```
