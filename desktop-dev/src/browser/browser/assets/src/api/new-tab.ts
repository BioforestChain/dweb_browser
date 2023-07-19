import { $AppMetaData, $WidgetMetaData } from "../types/app.type.ts";
import { nativeFetch } from "./fetch.ts";

export async function getAppInfo() {
  const res = await nativeFetch("/appsInfo");
  if (res.status !== 200) {
    console.error("请求失败：", res.status, res.statusText);
    return [];
  }
  return (await res.json()) as Promise<$AppMetaData[]>;
}

const html = String.raw;
const css = String.raw;
export async function getWidgetInfo() {
  return [
    {
      appId: `browser.dweb`,
      widgetName: "search",
      templateHtml: html`<form action="dweb:search" method="get" part="form">
        <input name="q" part="input" />
        <button type="submit" part="button btn-primary">
          <svg
            t="1689575320602"
            class="icon"
            viewBox="0 0 1024 1024"
            version="1.1"
            xmlns="http://www.w3.org/2000/svg"
            p-id="3759"
            width="1em"
            height="1em"
          >
            <path
              d="M887 840.4L673.4 624.8c41.8-52.4 67-118.8 67-191 0-169-137-306-306.2-306S128 265 128 434s137 306 306.2 306c73.2 0 140.2-25.6 193-68.4l212.2 214.2c6.4 6.8 15.2 10.2 23.8 10.2 8.2 0 16.4-3 22.6-9 13.2-12.6 13.6-33.4 1.2-46.6z m-452.8-166.2c-64.2 0-124.6-25-170-70.4-45.4-45.4-70.4-105.8-70.4-169.8 0-64.2 25-124.6 70.4-169.8 45.4-45.4 105.8-70.4 170-70.4s124.6 25 170 70.4c45.4 45.4 70.4 105.8 70.4 169.8 0 64.2-25 124.6-70.4 169.8-45.4 45.4-105.8 70.4-170 70.4z"
              p-id="3760"
            ></path>
          </svg>
        </button>
      </form>`,
      scopedStyle: css`
        form {
          display: flex;
          border: 1px solid rgba(0, 0, 0, 0.2);
          padding: 1em;
          border-radius: 1em;
          width: 100%;
          box-sizing: border-box;
        }
        input {
          flex: 1;
        }
        button {
          display: flex;
          align-items: center;
        }
      `,
      size: {
        row: 1,
        column: "100%",
      },
      sizeList: [
        {
          row: "100%",
          column: 1,
        },
      ],
    },
  ] as $WidgetMetaData[];
}

/**点击打开JMM */
export function clickApp(id: string) {
  nativeFetch("/openAppOrActivate", {
    search: {
      app_id: id,
    },
  });
}

/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
export function vibrateHeavyClick() {
  nativeFetch("/vibrateHeavyClick", {
    mmid: "haptics.sys.dweb",
  });
}

/**长按的退出按钮，这个会退出JMM后端 */
export async function quitApp(id: string) {
  await nativeFetch("/closeApp", {
    search: {
      app_id: id,
    },
    mmid: "jmm.browser.dweb",
  });
}

/**卸载的是jmm所以从这里调用 */
export async function deleteApp(id: string) {
  await quitApp(id);
  return await nativeFetch("/uninstall", {
    search: {
      app_id: id,
    },
    mmid: "jmm.browser.dweb",
  });
}

export async function detailApp(id: string) {
  return await nativeFetch("/detailApp", {
    search: {
      app_id: id,
    },
    mmid: "jmm.browser.dweb",
  });
}

export function shareApp(id: string) {
  nativeFetch("/shareApp", {
    search: {
      app_id: id,
    },
  });
}
