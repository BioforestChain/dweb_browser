import { $WidgetMetaData } from "../../types/app.type.ts";
import search_svg from "./search.svg?raw";
const html = String.raw;
const css = String.raw;

console.log("search_svg", search_svg);

export const searchWidget = {
  appId: `browser.dweb`,
  widgetName: "search",
  templateHtml: html`<form action="dweb:search" method="get" part="form">
    <input name="q" part="input glass" />
    <button type="submit" part="btn btn-primary">
      <span class="icon"> ${search_svg} </span>
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
      transition: 500ms;
      flex-basis: 50%;
    }
    input:focus {
      flex-basis: 100%;
    }
    button {
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
} satisfies $WidgetMetaData;
