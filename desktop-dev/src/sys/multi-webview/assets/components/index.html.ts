// 测试入口文件
import "./multi-webview-comp-status-bar.html.ts"
import "./multi-webview-comp-mobile-shell.html.ts";
import "./multi-webview-comp-navigator-bar.html.ts";
import "./multi-webview-comp-virtual-keyboard.html.ts";
import "./multi-webview-comp-toast.html.ts";
import "./multi-webview-comp-barcode-scanning.html.ts";
import "./multi-webview-comp-biometrics.html.ts";
import "./multi-webview-comp-haptics.html.ts";
import "./multi-webview-comp-share.html.ts"
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { query } from "lit/decorators.js";
import { state } from "lit/decorators.js";
import { getButtomBarState } from "./multi-webview-comp-safe-area.shim.ts"
import type { $BAR_STYLE, $BarState, $ShareOptions } from "../../types.ts";
import type { MultiWebViewCompMobileShell } from "./multi-webview-comp-mobile-shell.html.ts";

@customElement('root-comp')
export class RootComp extends LitElement{

  static override styles = createAllCSS()

  @query('multi-webview-comp-mobile-shell') multiWebviewCompMobileShell: MultiWebViewCompMobileShell | undefined | null;
  statusBarHeight = "38px";
  @property({type: Object}) statusBarState: $BarState = {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    insets: {
      top: parseInt(this.statusBarHeight),
      right: 0,
      bottom: 0,
      left: 0
    },
    overlay: false,
    visible: true
  }
  navigationBarHeight = "26px";
  @property({type: Object}) navigationBarState: $BarState = {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    insets: {
      top: 0,
      right: 0,
      bottom: parseInt(this.navigationBarHeight),
      left: 0
    },
    overlay: false,
    visible: true
  }
  @property({type: Object}) safeAreaState ={
    overlay: false
  }
  @property({type: Boolean}) isShowVirtualKeyboard = false;
  @property({type: Object}) virtualKeyboardState = {
    insets: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    },
    overlay: false,
    visible: false
  }
  @state() torchState = {isOpen: false}

  statusBarSetStyle(style: $BAR_STYLE){
    this.statusBarState = {
      ...this.statusBarState,
      style: style
    }
    return this;
  }

  statusBarSetBackground(color: string){
    this.statusBarState = {
      ...this.statusBarState,
      color: color
    }
    return this;
  }

  statusBarSetOverlay(overlay: boolean){
    this.statusBarState = {
      ...this.statusBarState,
      overlay: overlay
    }
    return this;
  }

  statusBarSetVisible(visible: boolean){
    this.statusBarState = {
      ...this.statusBarState,
      visible: visible
    }
    return this;
  }

  async statusBarGetState(){
    return {
      ...this.statusBarState,
      color: await this.hexaToRGBA(this.statusBarState.color)
    }
  }

  navigationBarSetStyle(style: $BAR_STYLE){
    this.navigationBarState = {
      ...this.navigationBarState,
      style: style
    }
    return this;
  }

  navigationBarSetBackground(color: string){
    this.navigationBarState = {
      ...this.navigationBarState,
      color: color
    }
    return this;
  }

  navigationBarSetOverlay(overlay: boolean){
    this.navigationBarState = {
      ...this.navigationBarState,
      overlay: overlay
    }
    return this;
  }

  navigationBarSetVisible(visible: boolean){
    this.navigationBarState = {
      ...this.navigationBarState,
      visible: visible
    }
    return this;
  }

  async navigationBarGetState(){
    return {
      ...this.navigationBarState,
      color: this.hexaToRGBA(this.navigationBarState.color)
    }
  }

  virtualKeyboardSetOverlay(overlay: boolean){
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      overlay: overlay
    }
    return this;
  }

  hexaToRGBA(str: string){
    return{
      red: parseInt(str.slice(1,3), 16),
      green: parseInt(str.slice(3,5), 16),
      blue: parseInt(str.slice(5,7), 16),
      alpha: parseInt(str.slice(7), 16)
    }
  }

  virtualKeyboardFirstUpdated(){
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      visible: true
    }
  }

  virtualKeyboardHideCompleted(){
    this.isShowVirtualKeyboard = false;
    console.error(`virtualKeybark 隐藏完成了 但是还没有处理`)
  }

  virtualKeyboardShowCompleted(){
    console.error('virutalKeyboard 显示完成了 但是还没有处理')
  }

  safeAreaGetState = () => {
    const bottomBarState = getButtomBarState(
      this.navigationBarState, 
      this.isShowVirtualKeyboard, 
      this.virtualKeyboardState
    );

    return {
      overlay: this.safeAreaState.overlay,
      insets: {
        left: 0,
        top: this.statusBarState.overlay ? this.statusBarState.insets.top : 0,
        right: 0,
        bottom: bottomBarState.overlay ? bottomBarState.insets.bottom : 0,
      },
      cutoutInsets: {
        left: 0,
        top: this.statusBarState.insets.top,
        right: 0,
        bottom: 0,
      },
      // 外部尺寸
      outerInsets: {
        left: 0,
        top: this.statusBarState.overlay ? 0 : this.statusBarState.insets.top,
        right: 0,
        bottom: bottomBarState.overlay ? 0 : bottomBarState.insets.bottom,
      }
    }
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    this
    .statusBarSetOverlay(overlay)
    .navigationBarSetOverlay(overlay)
    .virtualKeyboardSetOverlay(overlay)
  };

  torchToggleTorch(){
    this.torchState = {
      ...this.torchState,
      isOpen: !this.torchState.isOpen
    }
    return this;
  }

  barcodeScanningGetPhoto(){
    const el = document.createElement('multi-webview-comp-barcode-scanning');
    document.body.append(el)
  }

  biometricsMock(){
    this.multiWebviewCompMobileShell?.biometricsMock()
  }

  hapticsMock(){
    this.multiWebviewCompMobileShell?.hapticsMock('HEAVY');
  }

  shareShare(options: $ShareOptions){
    this.multiWebviewCompMobileShell?.shareShare(options)
  }

  // 之后的方法全部都是 测试代码
  virtualKeyboardShow (){
    this.isShowVirtualKeyboard = true;
  }

  virtualKeyboardHide(){
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      visible: false
    }
  }

  allocId = 0
  toast(){
    this.multiWebviewCompMobileShell?.toastShow(
      "message" + this.allocId++,
      `1000`,
      "top"
    );
  }

  testShare(){
    this.shareShare({
      title: "标题",
      text: "内容",
      link: "https://www.baidu.com",
      src: "https://img.tukuppt.com/photo-big/00/00/94/6152bc0ce6e5d805.jpg"
    })
  }

  testStatusbarSetBackground(){
    this.statusBarState = {
      ...this.statusBarState,
      color: "#FF0000FF"
    }
  }
  // 测试代码结束

  protected override render() {
    return html `
      <div class="root_comp">
        <multi-webview-comp-mobile-shell>
          <multi-webview-comp-status-bar 
            slot="status-bar" 
            ._color=${this.statusBarState.color}
            ._style = ${this.statusBarState.style}
            ._overlay = ${this.statusBarState.overlay}
            ._visible = ${this.statusBarState.visible}
            ._height = ${this.statusBarHeight}
            ._inserts = ${this.statusBarState.insets}
            ._torchIsOpen=${this.torchState.isOpen}
          ></multi-webview-comp-status-bar>
          <div class="role_app_content" slot="app_content"> 插入的 app content 的内容
            <button @click=${this.virtualKeyboardShow}>显示 virtual keyboard</button>
            <button @click=${this.virtualKeyboardHide}>隐藏 virtual keyboard</button>
            <button @click=${this.toast}>toast</button>
            <button @click=${() => this.safeAreaSetOverlay(true)}>设置 safe area overlay true</button>
            <button @click=${() => this.safeAreaSetOverlay(false)}>设置 safe area overlay false</button>
            <button @click=${this.torchToggleTorch}>切换手电状态 </button>
            <button @click=${this.barcodeScanningGetPhoto}>barcode scanning 选择文件</button>
            <button @click=${this.biometricsMock}>biometrics mock </button>
            <button @click=${this.hapticsMock}>haptics mock</button>
            <button @click=${this.testShare}>share</button>
            <button @click=${this.testStatusbarSetBackground}>设置 statusbar 背景色</button>
          </div>
          ${
            when(
              this.isShowVirtualKeyboard, 
              () => html`
                <multi-webview-comp-virtual-keyboard
                  slot="bottom-bar"
                  ._visible=${this.virtualKeyboardState.visible}
                  ._overlay=${this.virtualKeyboardState.overlay}
                  @first-updated=${this.virtualKeyboardFirstUpdated}
                  @hide-completed=${this.virtualKeyboardHideCompleted} 
                  @show-completed=${this.virtualKeyboardShowCompleted}
                ></multi-webview-comp-virtual-keyboard>
              `,
              () => {
                return html`
                  <multi-webview-comp-navigation-bar
                    slot="bottom-bar"
                    ._color=${this.navigationBarState.color}
                    ._style = ${this.navigationBarState.style}
                    ._overlay = ${this.navigationBarState.overlay}
                    ._visible = ${this.navigationBarState.visible}
                    ._height = ${this.navigationBarHeight}
                    ._inserts = ${this.navigationBarState.insets}
                  ></multi-webview-comp-navigation-bar>
                `
              },
            )
          }
        </multi-webview-comp-mobile-shell>
      </div>
    `
  }
}

// 生成 CSS 文件 给 styles
function createAllCSS (){
  return [
    css`
      .root_comp{
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        height: 100%;
      }
    `,

    css`
      .role_app_content{
        box-sizing: border-box;
        margin: 0px;
        padding: 0px;
        width: 100%;
        height: 100%;
        border: 1px solid red;
      }
    `
  ]
}

const elRootComp = new RootComp();
document.body.append(elRootComp);

 