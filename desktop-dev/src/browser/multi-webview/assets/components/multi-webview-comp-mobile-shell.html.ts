/**
 * app 容器组件
 * - status-bar
 * - ...其他插件都在这里注入
 * - 用来模拟移动端硬件外壳
 * - 仅仅只有 UI 不提供非 UI 之外的任何交互功能
 */
import { css, html, LitElement } from "lit";
import { customElement, query } from "lit/decorators.js";
import type { $ShareOptions } from "../../types.ts";

@customElement("multi-webview-comp-mobile-shell")
export class MultiWebViewCompMobileShell extends LitElement {
  static override styles = createAllCSS();
  @query(".app_content_container") appContentContainer:
    | HTMLDivElement
    | undefined
    | null;

  /**
   *
   * @param message
   * @param duration
   * @param position
   */
  toastShow(message: string, duration: string, position: "top" | "bottom") {
    const multiWebviewCompToast = document.createElement(
      "multi-webview-comp-toast"
    );
    [
      ["_message", message],
      ["_duration", duration],
      ["_position", position],
    ].forEach(([key, value]) => {
      multiWebviewCompToast.setAttribute(key as string, value);
    });
    this.appContentContainer?.append(multiWebviewCompToast);
  }

  setHostStyle() {
    const host = (this.renderRoot as ShadowRoot).host as HTMLElement;
  }

  biometricsMock() {
    const el = document.createElement("multi-webview-comp-biometrics");
    el.addEventListener("pass", () => {
      this.dispatchEvent(new Event("biometrices-pass"));
    });
    el.addEventListener("no-pass", () => {
      this.dispatchEvent(new Event("biometrices-no-pass"));
    });
    this.appContentContainer?.appendChild(el);
    console.log("biometrics", el, this.appContentContainer);
  }

  hapticsMock(text: string) {
    console.log("hapticsMock", text);
    const el = document.createElement("multi-webview-comp-haptics");
    el.setAttribute("text", text);

    this.appContentContainer?.appendChild(el);
  }

  shareShare(options: $ShareOptions) {
    const el = document.createElement("multi-webview-comp-share");
    const ui8 = options.body;
    const contentType = options.bodyType;
    const sparator = new TextEncoder()
      .encode(contentType.split("boundary=")[1])
      .join();
    const file = this.getFileFromUin8Array(ui8, sparator, 1);
    let src = "";
    let filename = "";

    if (file !== undefined) {
      if (
        file.name.endsWith(".gif") ||
        file.name.endsWith(".png") ||
        file.name.endsWith(".jpg") ||
        file.name.endsWith(".bmp") ||
        file.name.endsWith(".svg") ||
        file.name.endsWith(".webp")
      ) {
        src = URL.createObjectURL(file);
      } else {
        filename = file.name;
      }
    }
    [
      ["_title", options.title],
      ["_text", options.text],
      ["_link", options.link],
      ["_src", src],
      ["_filename", filename],
    ].forEach(([key, value]) => el.setAttribute(key, value));
    this.appContentContainer?.appendChild(el);
  }
  /**
   *  formData 原始 Uint8Array 数据
   *  separatorStr 在请求的headers里面的 multipart/form-data; boundary=----WebKitFormBoundarySLm2pLgOKCimWFjG boundary=后面的内容
   *  index file 数据保存在原始 formData 被分割后的位置
   *  这个位置会根据 formData参数的不同而不同
   */
  getFileFromUin8Array(
    rawUi8: Uint8Array,
    separatorStr: string,
    index: number
  ): File | undefined {
    const ui8Str = rawUi8.join();
    // const lineBreak = new TextEncoder().encode('\r\n').join()
    const dubleLineBreak = new TextEncoder().encode("\r\n\r\n").join();
    const resultNoPeratorStrArr = ui8Str.split(separatorStr);
    let contentType = "";
    let filename = "";
    let file: File | undefined = undefined;
    const str = resultNoPeratorStrArr[index];
    const arr = str.slice(7, -7).split(dubleLineBreak);
    arr.forEach((str: string, index: number) => {
      if (str.length === 0) return;
      if (index === 0) {
        const des = new TextDecoder().decode(
          new Uint8Array(
            str.slice(0, -1).split(",") as unknown as ArrayBufferLike
          )
        );
        des.split("\r\n").forEach((str, index) => {
          if (index === 0) {
            filename = str.split("filename=")[1].slice(1, -1);
          } else if (index === 1) {
            contentType = str.split(":")[1];
          }
        });
      } else {
        // 第二段文字是 内容 主要开头 , 和结尾的 ,12,10
        const s = str.slice(1, -6);
        const a = new Uint8Array(s.split(",") as unknown as ArrayBufferLike);
        const blob = new Blob([a], { type: contentType });
        file = new File([blob], filename);
      }
    });
    return file;
  }

  protected override render(): unknown {
    this.setHostStyle();
    return html`
      <div class="shell">
        <div class="shell_container">
          <slot name="status-bar"></slot>
          <div class="app_content_container">
            <slot name="app_content"> ... 桌面 ... </slot>
          </div>
          <slot name="bottom-bar"></slot>
        </div>
      </div>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        display: block;
        overflow: hidden;
        -webkit-app-region: resize;
      }
      .shell {
        padding: 0.8em 0.8em 0.8em 0.8em;
        border-radius: 2.6em;
        overflow: hidden;
        background: #000;
        height: 100%;
        width: 100%;
        box-sizing: border-box;
        -webkit-app-region: drag;
      }
      .shell_container {
        -webkit-app-region: no-drag;
        position: relative;
        display: flex;
        background: #fff;
        flex-direction: column;
        box-sizing: content-box;
        width: 100%;
        height: 100%;
        overflow: hidden;
        border-radius: 2em;
      }
      @media (prefers-color-scheme: dark) {
        .shell_container {
          background: #333;
        }
      }

      .app_content_container {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 100%;
      }
    `,
  ];
}
