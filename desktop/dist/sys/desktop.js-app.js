"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.string = exports.desktopJmm = void 0;
const micro_module_js_js_1 = require("./micro-module.js.js");
exports.desktopJmm = new micro_module_js_js_1.JsMicroModule({});
;
_view_id ?  : number;
async;
_bootstrap();
{
    this._view_id = await this.fetch(`file://mwebview.sys.dweb/open`).number();
}
async;
_shutdown();
{
    if (this._view_id !== undefined) {
        const view_id = this._view_id;
        this._view_id = undefined;
        void this.fetch(`file://mwebview.sys.dweb/close?view_id=${view_id}`);
    }
}
mmid = "desktop.sys.dweb";
