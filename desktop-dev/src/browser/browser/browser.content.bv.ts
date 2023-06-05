// 内容的 browserView
import process from "node:process";
import path from "node:path";
import { $BW } from "./browser.bw.ts";
import Electron from "electron";
import type { BrowserNMM } from "./browser.ts";

export function createCBV(
  this: BrowserNMM,
  bw: $BW,
  barHeight: number
): $CBV{
   
  const index_html = path.resolve(
    Electron.app.getAppPath(),
    "assets/browser/newtab/index.html"
  );
  const options = {
    webPreferences: {
      devTools: true,
      webSecurity: false,
      safeDialogs: true,
    }
  }
  
  const bv = new Electron.BrowserView(options)
  bv.setAutoResize({
    width: true,
    height: true,
    horizontal: true,
    vertical: true
  })

  const history = new History(bv);
 
  bv.loadWithHistory = (filename: string) => {
    const backState = history.state;
    history.pushState(new State(
      History.allcId,
      backState,
      filename,
      undefined
    ))
  }

  bv.canGoBack = () => {
    return history.state?.back !== undefined;
  }

  bv.goBack = () => {
    history.back()
  }

  bv.canGoForward = () => {
    return history.state?.forward !== undefined;
  }

  bv.goForward = () => {
    history.forward();
  }

  bv.reload = () => {
    const backState = history.state;
    bv.loadWithHistory(backState?.current!)
  }

  bv.loadWithHistory(index_html)
  bw.addBrowserView(bv)
  const [width, height] = bw.getContentSize();
  bv.setBounds({
    x: 0,
    y: bw.getTitleBarHeight() + barHeight ,
    width:  width,
    height: height - barHeight
  })
  
  // 调试状态下显示 开发工具栏
  process.argv.includes("--inspect") 
  ? bv.webContents.openDevTools()
  : '';
  
  {
    // 设置 userAgent 会导致 bv.webContents.canGoBack() canGoForward() 这样的方法无法返回正确的值
    // reload 这些都会不执行
    const userAgent = bv.webContents.getUserAgent()
    bv.webContents.setUserAgent(`${userAgent} dweb-host/${this.apiServer?.startResult.urlInfo.host}`)
  }

  return bv
}
 
export type $CBV = Electron.BrowserView & $ExtendsBrowserView
interface $ExtendsBrowserView{
  loadWithHistory(filename: string): void;
  canGoBack(): boolean;
  goBack(): void;
  canGoForward(): boolean;
  goForward(): void;
  reload(): void;
}

export class History{
  constructor(
    readonly bv: Electron.BrowserView
  ){}
  private states: State[] = [];
  private currentIndex = -1
  static  _allcId = 0;
  static get allcId(){
    return this._allcId++;
  }

  get length(){
    return this.states.length;
  }

  get state(){
    if(this.currentIndex === -1) return undefined;
    return this.states[this.currentIndex]
  }

  pushState(state: State){
    this.states = [...this.states.slice(0, this.currentIndex + 1), state]
    this.currentIndex++;
    if(this.state && this.state.back){
      this.state.back.forward = state
    }
    this.contentUpdatedByState(state)
  }

  back(){
    if(this.state === undefined) return;
    if(this.state.back === undefined) return;
    this.contentUpdatedByState(this.state.back)
    this.currentIndex--;
  }

  forward(){
    if(this.state === undefined) return;
    if(this.state.forward === undefined)return;
    this.contentUpdatedByState(this.state.forward)
    this.currentIndex++;
  }

  contentUpdatedByState(state: State){
    this.bv.webContents.stop()
    if(
      state.current.startsWith("http://")
      || state.current.startsWith('https://')  
    ){
      return this.bv.webContents.loadURL(state.current)
    }
    
    if(state.current.startsWith("/")){ /** 绝对路径开头的本地文件系统 */
      return this.bv.webContents.loadFile(state.current)
    }
    throw new Error(`History contentUpdatedByState 出现非法的 state.current=${state.current}`)
  }
}

class State {
  back: State | undefined;
  forward: State | undefined;
  constructor(
    readonly id: number,
    back: State | undefined,
    readonly current: string,
    forward: State | undefined,
  ){
    this.back = back;
    this.forward = forward;
  }

  backUpdate(state: State){
    this.back = state;
  }

  forwardUpdate(state: State){
    this.forward = state;
  }
}