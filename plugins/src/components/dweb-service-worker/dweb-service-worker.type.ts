import { FetchEvent, OnFetchEvent } from './FetchEvent.ts';
export interface DwebWorkerEventMap {
  updatefound: Event, // 更新或重启的时候触发
  pause: Event, // 监听应用暂停
  resume: Event, // 监听应用恢复
  fetch: FetchEvent,
  onFetch: OnFetchEvent
}

export interface UpdateControllerMap {
  start: Event, // 监听启动
  progress: string, // 进度每秒触发一次
  end: Event, // 结束
  cancel: Event, // 取消
}

export interface BFSMetaData {
  id: string,
  server: MainServer, // 打开应用地址
  title: string, // 应用名称
  subtitle: string, // 应用副标题
  icon: string, // 应用图标
  downloadUrl: string, // 下载应用地址
  images: string[], // 应用截图
  introduction: string, // 应用描述
  splashScreen: SplashScreen,
  author: string[], // 开发者，作者
  version: string, // 应用版本
  keywords: string[], // 关键词
  home: string, // 首页地址
  size: string, // 应用大小
  fileHash: string,
  permissions: string[],
  plugins: string[],
  releaseDate: string, // 发布时间
  /**
   * 静态网络服务
   */
  staticWebServers: StaticWebServer[],
  /**
   * 应用启动时会打开的网页
   * 要求 http/https 协议。
   * 它们会依此打开，越往后的层级越高
   *
   * TODO httpNMM 网关那边，遇到未知的请求，会等待一段时间，如果这段时间内这个域名被监听了，那么会将请求分发过去
   * 所以如果是 staticWebServers 定义的链接，那么自然而然地，页面会等到 staticWebServer 启动后得到响应，不会错过请求。
   */
  openWebViewList: OpenWebView[],
}

interface MainServer {
  /**
   * 应用文件夹的目录
   */
  root: string,
  /**
   * 入口文件
   */
  entry: string
}

interface StaticWebServer {
  /**
  * 应用文件夹的目录
  */
  root: string,
  /**
   * 入口文件
   */
  entry: string,
  subdomain: string,
  port: number,
}

interface OpenWebView {
  url: string
}
interface SplashScreen {
  entry: string
}
