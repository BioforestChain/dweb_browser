import { $WidgetCustomData } from "src/types/app.type.ts";
import { showToast } from "../api.ts";
import search_svg_raw from "./search.svg?raw";
const html = String.raw;
const css = String.raw;

export const searchWidget = {
  appId: `browser.dweb`,
  widgetName: "search",
  templateHtml: html`<form action="dweb://search" method="post" part="form" onsubmit="dwebSearch(event)">
    <input name="q" part="input glass ani" />
    <button type="submit" part="btn btn-primary">
      <span class="icon"> ${search_svg_raw} </span>
    </button>
  </form>`,
  scopedStyle: css`
    form {
      display: flex;
      justify-content: flex-end;
    }
    input {
      margin-inline-end: 0.5em;
      width: min-content !important;
      flex-basis: min(50%, 20em);
    }
    form:focus-within input {
      flex-basis: 100%;
    }
    .icon {
      transform: scale(1.25);
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
} satisfies $WidgetCustomData;

const startsWithIgnoreCase = (url: string) => {
  return /^dweb:/i.test(url);
};

Object.assign(globalThis, {
  dwebSearch(event: SubmitEvent) {
    const btnEle = event.submitter as HTMLButtonElement;
    const formEle = btnEle.form!;
    event.preventDefault();
    const formData = new FormData(formEle);
    const q = (formData.get("q") as string).trim();
    const method = formEle.method;
    let url: string;
    //  dweb://install?url=http://172.30.94.135:8096/metadata.json
    if (startsWithIgnoreCase(q)) {
      url = q.replace(/^dweb:/i, "dweb:");
    } else {
      const query = new URLSearchParams();
      formData.forEach((value, key) => {
        if (typeof value === "string") {
          query.append(key, value);
        }
      });
      url = formEle.action + "?" + query;
    }
    const xhr = new XMLHttpRequest();
    xhr.responseType = "text";

    xhr.onload = function () {
      if (xhr.readyState === xhr.DONE) {
        if (xhr.status !== 200) {
          showToast(`${xhr.status}:${xhr.statusText}`);
        }
      }
    };
    xhr.open(method, url);
    xhr.send((Date.now() + Math.random()).toString());

    return false;
  },
});
