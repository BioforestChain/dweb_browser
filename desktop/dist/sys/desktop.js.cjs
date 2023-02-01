"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.desktopJmm = void 0;
const micro_module_js_cjs_1 = require("./micro-module.js.cjs");
exports.desktopJmm = new micro_module_js_cjs_1.JsMicroModule("desktop.sys.dweb", {
    main_url: "desktop.sw.cjs",
});
//  {
//    metadata: { main_url: string; };
//    private _view_id?: number;
//    async _bootstrap() {
//       this._view_id = await this.fetch(
//          `file://mwebview.sys.dweb/open`
//       ).number();
//    }
//    async _shutdown() {
//       if (this._view_id !== undefined) {
//          const view_id = this._view_id;
//          this._view_id = undefined;
//          void this.fetch(`file://mwebview.sys.dweb/close?view_id=${view_id}`);
//       }
//    }
//    mmid = "desktop.sys.dweb" as const;
// }
