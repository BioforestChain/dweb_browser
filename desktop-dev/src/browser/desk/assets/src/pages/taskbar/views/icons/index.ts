// @ts-ignore
// @ts-ignore
// @ts-ignore
// @ts-ignore
// @ts-ignore
import layout_panel_top_svg from "./layout-panel-top.svg";

import { $AppIconInfo } from "src/components/app-icon/types.ts";

export const icons = {
  // anquanzhongxin,
  // kandianying,
  // naozhong,
  // quanbufenlei,
  // xiangji,
  layout_panel_top: {
    src: layout_panel_top_svg,
    maskable: false,
    monochrome: true,
    monocolor: "#FFF",
    monoimage: `linear-gradient(to bottom, #ffefba, #ffffff)`,
  },
} satisfies Record<string, $AppIconInfo>;
