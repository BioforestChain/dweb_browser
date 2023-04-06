import crypto from "node:crypto";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { IPC_ROLE } from "../../core/ipc/const.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import { ServerStartResult, ServerUrlInfo } from "./const.js";
import { defaultErrorResponse } from "./defaultErrorResponse.cjs";
import type { $DwebHttpServerOptions } from "./net/createNetServer.cjs";
import { Http1Server } from "./net/Http1Server.cjs";
import { PortListener } from "./portListener.cjs";
import { log } from "../../helper/devtools.cjs"
import type { IncomingMessage, OutgoingMessage } from "node:http";
import type http from "node:http";
 
import { IpcEvent } from "../../core/ipc/IpcEvent.cjs"
import url from "node:url";
import querystring from "node:querystring"
 
interface $Gateway {
  listener: PortListener;
  urlInfo: ServerUrlInfo;
  token: string;
}

export interface $BaseAction{
  action: string;
}

export type $BaseRouteMethod = "POST" | "GET" | "PUT"

export interface $BaseRoute{
  pathname: string;
  matchMode: "prefix" | "full";
  method: $BaseRouteMethod
}

/**
 * 路由
 */
 export interface $Route extends $BaseRoute{ 
  ipc: Ipc,
  res?: Map<string, OutgoingMessage>
}

// 过滤请求项
export interface $FilterActionItem extends $BaseAction{
  action: "filter/request";
  host: $MMID;
  urlPre: string;
}

export interface $AddRouteAddActionItem extends $BaseAction, $Route{
  action: "routes/add",
}

export interface $StateSendActionItem extends $BaseAction, $BaseRoute{
  action: "state/send",
  body: string | {[key: string]: number};
  headers?: {[key: string]: string},
  done: boolean; // 是否需要需要关闭 res
  to: string; // 发送给那个陈旭匹配的 插件
}

/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export class HttpServerNMM extends NativeMicroModule {
  mmid = `http.sys.dweb` as const;
  private _dwebServer = new Http1Server();

  private _tokenMap = new Map</* token */ string, $Gateway>();
  private _gatewayMap = new Map</* host */ string, $Gateway>();
  private _info: {
    hostname: string;
    port: number;
    host: string;
    origin: string;
    server: http.Server<typeof IncomingMessage, typeof http.ServerResponse>;
    protocol: {
        prefix: string;
        protocol: string;
        port: number;
    };
  } | undefined
  private _filterActions: Map<$FilterActionItem, Ipc> = new Map()
  private _filterActionsArray: $FilterActionItem[] = []
  private _filterActionResponse: Map<Ipc, Map<string,OutgoingMessage>> = new Map()
  private _routes: Map<Ipc, Map<string, $Route>> = new Map()

  protected async _bootstrap() {
    log.green(`${this.mmid} _bootstrap`)

    // 用来接受 推送的消息
    this.onConnect((remoteIpc) => {
      remoteIpc.onEvent((ipcEventMessage, ipc) => {
        const data = createBaseAction(ipcEventMessage.data);
        switch(data.action){
          case "routes/add":
            this.ipcEventOnAddRoutes(ipcEventMessage, ipc);
            break;
          case "state/send":
            this.ipcEventOnStateSend(ipcEventMessage, ipc);
            break;
          default: throw new Error(`[http-sever onConnect 还有没有匹配的 IpcEvent 处理方式] ${JSON.stringify(ipcEventMessage)}`)
        }
      })
    })

    // 创建了一个基础的 http 服务器 所有的 http:// 请求会全部会发送到这个地方来处理
    this._info = await this._dwebServer.create();
    this._info.server.on("request", async (req, res) => {
      res.setHeader("Access-Control-Allow-Origin", "*");  
      res.setHeader("Access-Control-Allow-Headers", "*");  
      res.setHeader("Access-Control-Allow-Methods","*");  
      /// 获取 host
      const host = this.getHostByReq(req)

      {
        // // 用来测测试 observe 
        // let pathname = url.parse(req.url as string).pathname
        
        // if(pathname?.includes('observe')){
          
        //   console.log(querystring.parse(req.url as string).mmid)
        //   console.log(req.headers)
        //   console.log(req.url)
        //   pathname = `/${querystring.parse(req.url as string).mmid}${pathname}`
        //   log.red(pathname)
        //   const o = {
        //     color: "#FFFFFFFF",
        //     style: "DEFAULT-",
        //     insets: {
        //         bottom: 148,
        //         left: 10,
        //         right: 0,
        //         top: 0,
        //     },
        //     overlay: false,
        //     visible: true
        //   } 
        //   // observe 会写是要 \n 有一个换行符，UI 才可以实现 for 循环获取数据
        //   // 
        //   setInterval(() => {
        //     log.red('回写了')

        //     const encode = new TextEncoder().encode(JSON.stringify(o)+"\n")
        //     res.write(encode)
        //   },3000)
        //   return;
        // }
      }
      

      // 是否有匹配的路由 拦截路由 分发请求
      {
        if(await this.distributeRequest(req, res)){
          return;
        }
      }
       
      {
        // 在网关中寻址能够处理该 host 的监听者
        const gateway = this._gatewayMap.get(host);
        if (gateway == undefined) {
          log.red(`[http-server onRequest 既没分发也没有匹配 gatewaty请求] ${req.url}`)
          return defaultErrorResponse(
            req,
            res,
            502,
            "Bad Gateway",
            "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
          );
          
        }
        
        // gateway.listener.ipc.request("/on-connect")
  
        // const gateway_timeout = setTimeout(() => {
        //   if (res.writableLength === 0) {
        //   }
        //   res.write;
        //   res.hasHeader;
        // }, 3e4 /* 30s 没有任何 body 写入的话，认为网关超时 */);
        // 源代码
        void gateway.listener.hookHttpRequest(req, res);
      }
    });


    /// 监听 IPC 请求 /start
    this.registerCommonIpcOnMessageHandler({
      pathname: "/start",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "object",
      handler: async (args, ipc) => {
        return await this.start(ipc, args);
      },
    });

    /// 监听 IPC 请求 /close
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { port: "number?", subdomain: "string?" },
      output: "boolean",
      handler: async (args, ipc) => {
        return await this.close(ipc, args);
      },
    });

    /// 监听 IPC 请求 /listen post
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/listen",
      matchMode: "full",
      input: { token: "string", routes: "object" },
      output: "object",
      handler: async (args, ipc, message) => {
        return this.listen(args.token, message, args.routes as $ReqMatcher[]);
      },
    });
  }
  protected _shutdown() {
    this._dwebServer.destroy();
  }

  private getServerUrlInfo(ipc: Ipc, options: $DwebHttpServerOptions) {
    const mmid = ipc.remote.mmid;
    const { subdomain: options_subdomain = "", port = 80 } = options;
    const subdomainPrefix =
      options_subdomain === "" || options_subdomain.endsWith(".")
        ? options_subdomain
        : `${options_subdomain}.`;
    if (port <= 0 || port >= 65536) {
      throw new Error(`invalid dweb http port: ${port}`);
    }

    const public_origin = this._dwebServer.origin;
    const host = `${subdomainPrefix}${mmid}:${port}`;
    const internal_origin = `http://${subdomainPrefix}${mmid}-${port}.${this._dwebServer.authority}`;
    return new ServerUrlInfo(host, internal_origin, public_origin);
  }

  /** 申请监听，获得一个连接地址 */
  private async start(ipc: Ipc, hostOptions: $DwebHttpServerOptions) {
    const serverUrlInfo = this.getServerUrlInfo(ipc, hostOptions);
    if (this._gatewayMap.has(serverUrlInfo.host)) {
      throw new Error(`already in listen: ${serverUrlInfo.internal_origin}`);
    }
    const listener = new PortListener(
      ipc,
      serverUrlInfo.host,
      serverUrlInfo.internal_origin
    );
    /// ipc 在关闭的时候，自动释放所有的绑定
    listener.onDestroy(
      ipc.onClose(() => {
        this.close(ipc, hostOptions);
      })
    );
    // jmmMetadata.sys.dweb-80.localhost:22605
    // jmmmetadata.sys.dweb-80.localhost:22605
    const token = Buffer.from(
      crypto.getRandomValues(new Uint8Array(64))
    ).toString("base64url");
    const gateway: $Gateway = { listener, urlInfo: serverUrlInfo, token };
    this._tokenMap.set(token, gateway);
    this._gatewayMap.set(serverUrlInfo.host, gateway);
    return new ServerStartResult(token, serverUrlInfo);
  }

  /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
  private async listen(
    token: string,
    message: IpcRequest,
    routes: $ReqMatcher[]
  ) {
    const gateway = this._tokenMap.get(token);
    if (gateway === undefined) {
      throw new Error(`no gateway with token: ${token}`);
    }

    const streamIpc = new ReadableStreamIpc(
      gateway.listener.ipc.remote,
      IPC_ROLE.CLIENT
    );
    void streamIpc.bindIncomeStream(message.body.stream());

    streamIpc.onClose(
      gateway.listener.addRouter({
        routes,
        streamIpc,
      })
    );

    return new Response(streamIpc.stream, { status: 200 });
  }
  /**
   * 释放监听
   */
  private close(ipc: Ipc, hostOptions: $DwebHttpServerOptions) {
    const serverUrlInfo = this.getServerUrlInfo(ipc, hostOptions);

    const gateway = this._gatewayMap.get(serverUrlInfo.host);
    if (gateway === undefined) {
      return false;
    }
    this._tokenMap.delete(gateway.token);
    this._gatewayMap.delete(serverUrlInfo.host);
    /// 执行销毁
    gateway.listener.destroy();

    return true;
  }

  // 获取 host
  getHostByReq = (req: IncomingMessage) => {
    /// 获取 host
    var header_host: string | null = null;
    var header_x_dweb_host: string | null = null;
    var header_user_agent_host: string | null = null;
    var query_x_web_host: string | null = new URL(
      req.url || "/",
      this._dwebServer.origin
    ).searchParams.get("X-Dweb-Host");
    for (const [key, value] of Object.entries(req.headers)) {
      switch (key) {
        case "host":
        case "Host": {
          if (typeof value === "string") {
            header_host = value;
            /// 桌面模式下，我们没有对链接进行拦截，将其转化为 `public_origin?X-Dweb-Host` 这种链接形式 ，因为支持 *.localhost 通配符这种域名
            /// 所以这里只需要将 host 中的信息提取出来
            if (value.endsWith(`.${this._dwebServer.authority}`)) {
              query_x_web_host = value
                .slice(0, -this._dwebServer.authority.length - 1)
                .replace(/-(\d+)/, ":$1");
            }
          }
          break;
        }
        case "x-dweb-host":
        case "X-Dweb-Host": {
          if (typeof value === "string") {
            header_x_dweb_host = value;
          }
        }
        case "user-agent":
        case "User-Agent": {
          if (typeof value === "string") {
            const host = value.match(/\sdweb-host\/(.+)\s*/)?.[1];
            if (typeof host === "string") {
              header_user_agent_host = host;
            }
          }
        }
      }
    }
  
    let host =
      query_x_web_host ||
      header_x_dweb_host ||
      header_user_agent_host ||
      header_host;
    if (typeof host === "string" && host.includes(":") === false) {
      host += ":" + this._info?.protocol.port;
    }
    if (typeof host !== "string") {
      /** 如果有需要，可以内部实现这个 key 为 "*" 的 listener 来提供默认服务 */
      host = "*";
    }
    return host;
  }

  // 添加路由的操作
  ipcEventOnAddRoutes(ipcEventMessage: IpcEvent, remoteIpc: Ipc) {
    const data = createAddRoutesActionItem(ipcEventMessage.data)
    // 一个 ipc 对应一个map
    let _map = this._routes.get(remoteIpc)
    if(_map === undefined){
      _map = new Map()
      this._routes.set(remoteIpc, _map)
    }
    // 重复添加只保留一个
    const key = createRouteKey(data)
    _map.set(key, {
      pathname: data.pathname,
      matchMode: data.matchMode,
      method: data.method,
      ipc: remoteIpc,
    })
    const route = _map.get(key)
  }

  // 分发请求
  distributeRequest = async (req: IncomingMessage, res: OutgoingMessage) => {
    // console.log(req.url)
    // console.log(req.headers)
    let has = false;
    let pathname = url.parse(req.url as string,).pathname;
    if(pathname === null) return;
    if(pathname.endsWith("observe")){
      pathname = `/${querystring.parse(req.url as string).mmid}${pathname}`
    }
    log.green(`[http-server.cts 接受的到了请求] http://${req.headers.host}${pathname}`)

    const full = createRouteKeyByArgs(
      pathname as string,
      "full",
      req.method as $BaseRouteMethod
    )
    const prefix = createRouteKeyByArgs(
      pathname as string,
      'prefix',
      req.method as $BaseRouteMethod
    )
    const parentRoutes = this._routes.values();
    let loop = true
    do{
      const {value, done} = parentRoutes.next() 
      loop = !done
      if(done) continue;
      let route = value.get(full)
      if(route !== undefined){
        route.ipc.postMessage(
          IpcEvent.fromText(
            'request/distribute',
            await createDistributeRequestData(route, req)
          )
        )
        has = true;
        
        // 保存 res
        let _res = route.res;
        if(!_res){
          _res = new Map()
          route.res = _res;
        }

        _res.set(req.headers.origin as string, res)
      }

      route = value.get(prefix)
      if(route !== undefined){
        route.ipc.postMessage(
          IpcEvent.fromText(
            'request/distribute',
            await createDistributeRequestData(route, req)
          )
        )
        has = true;
        // 保存 res
        let _res = route.res;
        if(!_res){
          _res = new Map()
          route.res = _res;
        }
        _res.set(req.headers.origin as string, res)
        // console.log('route.res: ', route.res?.keys())
      }

    }while(loop)
    return has;
  }

  // 把数据通过 res 发送给匹配的对象
  ipcEventOnStateSend = (ipcEventMessage: IpcEvent, ipc: Ipc) => {
    const data = createStateSendActionItem(ipcEventMessage.data);
    const parentRoutes = this._routes.values()
    let loop = true;
    // console.log('http-server ipcEventOnStateSend 准备发送数据', data, data.body, Object.prototype.toString.call(data.body).slice(8, -1))
    do{
      const {value, done} = parentRoutes.next() 
      loop = !done
      if(done) continue;
      const routes = value.values()
      let insideLoop = true;
      do{
        const {value, done} = routes.next()
        insideLoop = !done
        if(done) continue;
        if(createRouteKey(value) !== createRouteKey(data)) continue;
        const res = value.res?.get(data.to)
        if(!res) continue;
        let _data: any;
        if(data.headers?.bodyType === "JSON"){
          _data = data.body
        }else if(data.headers?.bodyType === "Uint8Array"){
          _data = Uint8Array.from(Object.values(data.body));
        }else if(data.headers?.bodyType === "Object"){
          _data = JSON.stringify(data.body)
        }else if(data.headers?.bodyType === "string"){
          _data = data.body
        }else{
          log.red(`[http-server ipcEventOnStateSend 非法的 bodyType]`)
          console.log(data)
          throw new Error(`http-server ipcEventOnStateSend 非法的 bodyType ${data.headers?.bodyType}`)
        }
        res.write(_data)
        if(!data.done)continue;
        res.end()
        value.res?.delete(data.to)
      }while(insideLoop)
    }while(loop)
  }
}

function createBaseAction(data: string | Uint8Array){
  if(Array.isArray(data)) throw new Error('[http-sever.cts createBaseAction 非法的参数 data 只能够是JSON字符串]')
  try{
    const o = JSON.parse(data as string) as $BaseAction;
    return o
  }catch(err){
    throw err;
  }
}

function createFilterActionItem(data: string | Uint8Array){
  if(Array.isArray(data)) throw new Error('[http-sever.cts createFilterActionItem 非法的参数 data 只能够是JSON字符串]')
  try{
    const o = JSON.parse(data as string) as $FilterActionItem;
    return o
  }catch(err){
    throw err;
  }
}

function createAddRoutesActionItem(data: string | Uint8Array){
  if(Array.isArray(data)) throw new Error('[http-sever.cts createAddRoutesActionItem 非法的参数 data 只能够是JSON字符串]')
  try{
    const o = JSON.parse(data as string) as $AddRouteAddActionItem;
    return o
  }catch(err){
    throw err;
  }
}

function createStateSendActionItem(data: string | Uint8Array){
  if(Array.isArray(data)) throw new Error('[http-sever.cts createStateSendActionItem 非法的参数 data 只能够是JSON字符串]')
  try{
    const o = JSON.parse(data as string) as $StateSendActionItem;
    return o
  }catch(err){
    throw err;
  }
}

async function createDistributeRequestData(
  route: $Route,
  req: IncomingMessage
): Promise<string>{
  return new Promise(resolve => {
    let data: Uint8Array = new Uint8Array()
    if(req.method === "POST"){
      req.on('data', (chunk) => {
        data = Uint8Array.from([...data, ...chunk])
      })
      req.on('end', () => {
        resolve(JSON.stringify({
          pathname: route.pathname,
          method: req.method,
          url: req.url,
          headers: req.headers,
          matchMode: route.matchMode,
          body: new TextDecoder().decode(data)
        }))
      })
    }else{
      resolve(JSON.stringify({
        pathname: route.pathname,
        method: req.method,
        url: req.url,
        headers: req.headers,
        matchMode: route.matchMode,
        body: null
      }))
    }

  })
}


function createRouteKey(
  route: $BaseRoute 
){
  return `${route.pathname}${route.matchMode}${route.method}`
}

function createRouteKeyByArgs(
  pathname: string,
  matchMode: string,
  method: string,
){
  return `${pathname}${matchMode}${method}`
}




