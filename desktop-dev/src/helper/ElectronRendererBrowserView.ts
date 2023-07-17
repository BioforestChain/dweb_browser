/// <reference lib="dom"/>
import { Remote, releaseProxy } from "comlink";
import { LitElement, PropertyValueMap, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { importApis } from "./openNativeWindow.preload.ts";

// TODO 这里补充真实的类型
const apis = importApis<any>();

const TAG = "electron-browser-view";
Electron.ipcRenderer;

/**
 * 在Web中渲染BrowserView，因为是原生的视图，所以它一定是渲染于整个视图上方
 * 这是给 Renderer 环境使用的，依赖于 openNativeWindow.preload 来加载和原生通讯的接口
 *
 * @TODO 这个组件应该脱离lit？用更加低运行时成本的方案来实现？
 */
@customElement(TAG)
export class ElectronBrowserView extends LitElement {
  static override styles = [
    css`
      :host {
        box-sizing: border-box;
        margin: 0px;
        padding: 0px;
        width: 100%;
        height: 100%;
        display: block;
      }
    `,
  ];

  @property({ type: Number }) zIndex = -1;
  @property({ type: String }) src = "";
  @property({ type: String }) backgroundColor = "rgba(255, 255, 255, 0)";
  @property({ type: String }) preload = "";
  @property({ type: Boolean }) devtools = false;

  private createPo?: Promise<Electron.BrowserView>;
  private browserViewRemote?: Remote<Electron.BrowserView>;
  private browserView?: Electron.BrowserView;
  override async connectedCallback() {
    super.connectedCallback();
    this.createPo = apis
      .createBrowserView({
        webPreferences: {
          devTools: true,
          preload: this.preload,
        },
      })
      .then((res?: Remote<Electron.BrowserView>) => {
        this.browserViewRemote = res;
        return res;
      });
    /// 等待创建完成
    const browserView = (this.browserView = this.browserViewRemote as unknown as Electron.BrowserView);
    /// 获取zindex
    this.zIndex = await apis.getBrowserViewZIndex(browserView);
    /// 开始监听DOM布局，将其布局位置同步给browserview
    this._startWatchResize(browserView);
    /// 设置背景颜色
    browserView.setBackgroundColor(this.backgroundColor);
    /// 加载设置的url
    if (this.src) {
      browserView.webContents.loadURL(this.src);
    }
    /// 开关开发者工具
    if (this.devtools) {
      browserView.webContents.openDevTools();
    } else {
      browserView.webContents.closeDevTools();
    }
  }

  override willUpdate(changes: PropertyValueMap<any>) {
    super.willUpdate(changes);
    const { browserView } = this;
    if (browserView === undefined) {
      return;
    }
    if (changes.has("src")) {
      browserView.webContents.loadURL(this.src);
    }
    if (changes.has("devtools")) {
      if (this.devtools) {
        browserView.webContents.openDevTools();
      } else {
        browserView.webContents.closeDevTools();
      }
    }
    if (changes.has("zIndex")) {
      apis.setBrowserViewZIndex(browserView, this.zIndex);
    }
  }

  override async disconnectedCallback() {
    super.disconnectedCallback();
    /// 等待 browserView 创建返回
    const browserView = this.browserView || (await this.createPo);
    /// 停止监听DOM布局
    this._stopWatchResize();

    if (browserView) {
      /// 删除browserView视图
      await apis.deleteBrowserView(browserView);
      /// 释放 remote 对象
      await this.browserViewRemote?.[releaseProxy]();
      /// 清除引用
      this.browserView = undefined;
      this.browserViewRemote = undefined;
    }
  }

  private _resizeOb?: ResizeObserver;
  private _stopWatchResize() {
    if (this._resizeOb) {
      this._resizeOb.unobserve(this);
      this._resizeOb.disconnect();
      this._resizeOb = undefined;
    }
  }
  private _startWatchResize(browserView: Electron.BrowserView) {
    this._stopWatchResize();

    this._resizeOb = new ResizeObserver(() => {
      const rect = this.getBoundingClientRect();
      const bounds: Electron.Rectangle = {
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
      };
      browserView.setBounds(bounds);
    });
    this._resizeOb.observe(this);
  }

  override render() {
    // useragent=${navigator.userAgent + "dweb-host/" + location.host}
    return html``;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: ElectronBrowserView;
  }
}
