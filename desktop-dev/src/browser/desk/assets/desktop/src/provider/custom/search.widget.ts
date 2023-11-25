import { $WidgetCustomData } from "src/types/app.type.ts";
import search_svg_raw from "./search.svg?raw";
const html = String.raw;
const css = String.raw;

export const searchWidget = {
  appId: `browser.dweb`,
  widgetName: "search",
  templateHtml: html`<form action="dweb://search" method="get" part="form" onsubmit="dwebSearch(event)">
    <input name="q" part="input glass ani" />
    <button type="submit" part="btn btn-primary">
      <span class="icon"> ${search_svg_raw} </span>
    </button>
  </form>`,
  scopedStyle: css`
    form {
      display: flex;
      justify-content: center;
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

Object.assign(globalThis, {
  dwebSearch(event: SubmitEvent) {
    const btnEle = event.submitter as HTMLButtonElement;
    const formEle = btnEle.form!;
    event.preventDefault();
    const formData = new FormData(formEle);
    const q = formData.get("q") as string;
    const method = formEle.method;
    let url: string;
    if (q.startsWith("dweb:")) {
      url = q;
    } else {
      const query = new URLSearchParams(formData).toString();
      url = formEle.action + "?" + query;
    }
    const xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.send();
    return false;
  },
});
