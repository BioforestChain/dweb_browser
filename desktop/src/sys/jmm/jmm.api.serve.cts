
import { Ipc, IpcRequest, IpcResponse } from "../../core/ipc/index.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
import type { JmmNMM } from "./jmm.cjs";

export async function createApiServer(
  this: JmmNMM
){
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
    port: 6363
  });

  // 

  const streamIpc = await this.apiServer.listen();
  streamIpc.onRequest(onRequest.bind(this))
}

async function onRequest(
  this: JmmNMM,
  request: IpcRequest,
  ipc: Ipc
){
  const path = request.parsed_url.pathname;
  switch(request.parsed_url.pathname){
    case "/get_data":
      return getData.bind(this)(request, ipc)
    break;
    default: {
      throw new Error(`${this.mmid} 有没有处理的pathname === ${request.parsed_url.pathname}`)
    }
  }
  const url = request.url
}

async function getData(
  this: JmmNMM,
  request: IpcRequest,
  ipc: Ipc
){
  const searchParams = request.parsed_url.searchParams;
  const url = searchParams.get('url');
  if(url === null) throw new Error(`${this.mmid} url === null`)
  const res = await fetch(url)
  ipc.postMessage(
    await IpcResponse.fromResponse(
      request.req_id,
      res,
      ipc,
      true
    )
  )
}