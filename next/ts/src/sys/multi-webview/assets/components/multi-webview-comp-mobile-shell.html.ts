/**
 * app 容器组件
 * - status-bar
 * - ...其他插件都在这里注入
 * - 用来模拟移动端硬件外壳
 * - 仅仅只有 UI 不提供非 UI 之外的任何交互功能
 */
import { css, html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { query } from "lit/decorators.js";
import type { $ShareOptions } from "../../types.ts";

@customElement("multi-webview-comp-mobile-shell")
export class MultiWebViewCompMobileShell extends LitElement{

  static override styles = createAllCSS();
  @query('.app_content_container') appContentContainer: HTMLDivElement | undefined | null;

  /**
   * 
   * @param message 
   * @param duration 
   * @param position 
   */
  toastShow(
    message: string,
    duration: string,
    position: "top" | "bottom"
  ){
    const multiWebviewCompToast = document.createElement('multi-webview-comp-toast');
    [
      ["_message", message],
      ["_duration", duration],
      ["_position", position],
    ].forEach(([key, value])=>{
      multiWebviewCompToast.setAttribute(key as string, value)
    })
    this.appContentContainer?.append(multiWebviewCompToast)
  }

  setHostStyle(){
    const host = ((this.renderRoot as ShadowRoot).host as HTMLElement);
  }

  biometricsMock(){
    const el = document.createElement('multi-webview-comp-biometrics')
    el.addEventListener('pass', () => {
      this.dispatchEvent(new Event("biometrices-pass"))
    })
    el.addEventListener('no-pass', () => {
      this.dispatchEvent(new Event('biometrices-no-pass'))
    })
    this.appContentContainer?.appendChild(el)
    console.log('biometrics', el, this.appContentContainer)
  }

  hapticsMock(text: string){
    console.log("hapticsMock", text)
    const el = document.createElement('multi-webview-comp-haptics');
          el.setAttribute('text', text)
          
    this.appContentContainer?.appendChild(el)
  }

  shareShare(
    options: $ShareOptions
  ){
    const el = document.createElement('multi-webview-comp-share');
    [
      ["_title", options.title],
      ["_text", options.text],
      ["_link", options.link],
      ["_src", options.src]
    ].forEach(([key, value]) => el.setAttribute(key, value))
    this.appContentContainer?.appendChild(el);
  }

  protected override render(): unknown {
    this.setHostStyle()
    return html`
      <div class="shell_container">
        <slot name="status-bar"></slot>
        <div class="app_content_container">
          <slot name="app_content">
            ... 桌面 ...
          </slot>
        </div>
        <slot name="bottom-bar"></slot>
      </div>
    `
  }
}

function createAllCSS (){
  return [
    css`
      :host{
        overflow: hidden;
      }

      .shell_container{
        --width: 375px;
        position: relative;
        display: flex;
        flex-direction: column;
        box-sizing: content-box;
        width: var(--width);
        height: calc(var(--width) * 2.05);
        border: 10px solid #000000;
        border-radius: calc(var(--width) / 12);
        overflow: hidden;
      }

      .app_content_container{
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 100%;
      }
    `
  ]
}

