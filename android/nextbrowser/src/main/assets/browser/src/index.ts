/// <reference lib="dom"/>
/// <reference path="../../node_modules/vite/client.d.ts"/>

import { CSSResultGroup, LitElement, html, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import { createRef, ref } from "lit/directives/ref.js";
// @ts-ignore
import { WebviewWindow, WindowFrame } from "./WebviewWindow.ts";
import dweb_browser_css from "./index.css?raw";
import "./types.ts";

@customElement("dweb-browser")
export class BrowserElement extends LitElement {
  constructor() {
    super();
    queueMicrotask(() => {
      for (const id of __web_browser_api__.getAllWindow().split(",")) {
        const win = new WebviewWindow(+id, () => this.requestUpdate());
        this.windows.set(id, win);
        this.requestUpdate();
      }
    });
  }
  @state()
  private _url = "https://baidu.com";

  @state()
  private windows = new Map<string, WebviewWindow>();
  private _openWindow = () => {
    const webviewWindow = new WebviewWindow(
      __web_browser_api__.createWindow(this._url),
      () => this.requestUpdate()
    );
    this.windows.set(webviewWindow.id.toString(), webviewWindow);
    this._selected = webviewWindow;
    this.requestUpdate();
  };
  @state()
  private _selected?: WebviewWindow;
  private _selectWindow = (event: Event) => {
    this._selected = this.windows.get(
      (event.target as HTMLSelectElement).value
    );
  };
  static styles?: CSSResultGroup | undefined = [unsafeCSS(dweb_browser_css)];
  readonly MAX_HEIGHT = window.innerHeight;
  readonly MAX_WIDTH = window.innerWidth;
  readonly MAX_X = window.innerWidth;
  readonly MAX_Y = window.innerHeight;
  private _frameUpdater = (key: keyof WindowFrame) => {
    return (event: InputEvent) => {
      this._selected![key] = (event.target as HTMLInputElement).valueAsNumber;
      this.requestUpdate();
    };
  };

  private _windowsRef = createRef<HTMLDivElement>();
  private _barsRef = createRef<HTMLDivElement>();
  private _barbufferStartRef = createRef<HTMLDivElement>();

  firstUpdated() {
    console.log("this._barsRef.value", this._barsRef.value);
    // @ts-ignore
    const timeline = new ScrollTimeline({
      source: this._barsRef.value,
      //   source: this._barsRef.value,
      axis: "inline",
    });
    const windowsEle = this._windowsRef.value!;
    let preCount = 0;
    let preAni: undefined | Animation;
    const barBufferSize = 0.2; // 20vw
    const barSize = 0.8; // 80vw
    const installScrolAnimation = (count: number) => {
      preCount = count;
      preAni?.cancel();
      //   if (count === 0) {
      //     return;
      //   }
      const totalScrollSize = count * barSize + barBufferSize * 2 - 1;
      const bufferPrecent = barBufferSize / totalScrollSize / 2;

      const totalOffsetX = (count - 1) * -100;
      const endTranslateX = `translateX(${totalOffsetX}%)`;
      //   const keyframes: Keyframe[] = [];
      //   for (let f = 1; f < count; f++) {
      //     keyframes.push({
      //       offset: (f + 1) / count - bufferPrecent,
      //       transform: `translateX(${((f + 1) / count) * totalOffsetX}%)`,
      //     });
      //   }
      const aniKeyframes = [
        { offset: 0, transform: `translateX(0%)` },
        {
          offset: bufferPrecent,
          transform: `translateX(0%)`,
        },
        // ...keyframes,
        {
          offset: 1 - bufferPrecent,
          transform: endTranslateX,
        },
        { offset: 1, transform: endTranslateX },
      ];
      console.table(aniKeyframes);
      preAni = windowsEle.animate(aniKeyframes, {
        fill: "both",
        // @ts-ignore
        timeline,
      });
    };
    const syncToNative = () => {
      const windowEleList =
        windowsEle.querySelectorAll<HTMLDivElement>(".window");
      /// 如果元素数量改变，那么重新安装动画
      if (windowEleList.length !== preCount) {
        installScrolAnimation(windowEleList.length);
      }
      windowEleList.forEach((ele) => {
        const window = this.windows.get(ele.dataset.id!);
        if (window === undefined) {
          return;
        }
        const domFrame = ele.getBoundingClientRect();
        // window.x = domFrame.x + domFrame.width / 2 - window.width / 2;
        // window.y = domFrame.y + domFrame.height / 2 - window.height / 2;
        window.width = domFrame.width;
        window.height = domFrame.height;
        window.x = domFrame.x;
        window.y = domFrame.y;
      });
      requestAnimationFrame(syncToNative);
    };
    syncToNative();
  }
  render() {
    return html`
      <!-- 调试面板 -->
      <div class="debug-pannel">
        <input
          .value=${this._url}
          @input=${(event: InputEvent) => {
            this._url = (event.target as HTMLInputElement).value;
          }}
        />
        <br />
        <button @click="${this._openWindow}">打开新窗口</button>
        <select @change=${this._selectWindow} .value=${this._selected?.id}>
          <option value="undefined"><i>请选择窗口</i></option>
          ${[...this.windows].map(
            (ww, id) => html`<option value="${id}">${id}</option>`
          )}
        </select>
        <form class="panel">
          <fieldset .disabled=${!this._selected}>
            <label for="win_x">
              <span class="value">X: ${this._selected?.x}pt</span>
              <input
                id="win_x"
                type="range"
                .value=${this._selected?.x}
                min="0"
                max=${this.MAX_X}
                step="1"
                @input=${this._frameUpdater("x")}
              />
            </label>
            <label for="win_y">
              <span class="value">Y : ${this._selected?.y}pt</span>
              <input
                id="win_y"
                type="range"
                .value=${this._selected?.y}
                min="0"
                max=${this.MAX_Y}
                step="1"
                @input=${this._frameUpdater("y")}
              />
            </label>
            <label for="win_width">
              <span class="value">Width: ${this._selected?.width}pt</span>
              <input
                id="win_width"
                type="range"
                .value=${this._selected?.width}
                max=${this.MAX_WIDTH}
                min="0"
                step="1"
                @input=${this._frameUpdater("width")}
              />
            </label>
            <label for="win_height">
              <span class="value">Height: ${this._selected?.height}pt</span>
              <input
                id="win_height"
                type="range"
                .value=${this._selected?.height}
                min="0"
                max=${this.MAX_HEIGHT}
                step="1"
                @input=${this._frameUpdater("height")}
              />
            </label>
            <label for="win_round">
              <span class="value">Round: ${this._selected?.round}pt</span>
              <input
                id="win_round"
                type="range"
                .value=${this._selected?.round}
                min="0"
                max="100"
                step="1"
                @input=${this._frameUpdater("round")}
              />
            </label>
          </fieldset>
        </form>
      </div>
      <div class="view">
        <div class="windows-view">
          <div ${ref(this._windowsRef)} class="windows">
            ${[...this.windows].map(([id, win]) => {
              return html`<div data-id=${id} class="window"></div>`;
            })}
          </div>
        </div>
        <div class="bars-view">
          <div class="bars" ${ref(this._barsRef)}>
            <span class="bar-buffer start"></span>
            ${[...this.windows].map(([id, win]) => {
              return html`<form
                class="bar"
                @submit=${(event: SubmitEvent) => {
                  event.preventDefault();
                  const formEle = event.target as HTMLFormElement;
                  const urlInputEle = formEle.elements.namedItem(
                    "url"
                  ) as HTMLInputElement;
                  win.url = urlInputEle.value;
                }}
              >
                <input
                  name="url"
                  class="url-inputer"
                  .value=${win.url}
                  data-url=${win.url}
                />
              </form>`;
            })}
            <span ${ref(this._barbufferStartRef)} class="bar-buffer end"></span>
          </div>
          <div class="buttons">
            <button class="item">后退</button>
            <button class="item">前进</button>
            <button class="item" @click=${this._openWindow}>新增</button>
            <button class="item">缩略</button>
            <button class="item">菜单</button>
          </div>
        </div>
      </div>
    `;
  }
}
