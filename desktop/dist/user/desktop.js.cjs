"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.desktopJmm = void 0;
const micro_module_js_cjs_1 = require("../sys/micro-module.js.cjs");
exports.desktopJmm = new micro_module_js_cjs_1.JsMicroModule("desktop.sys.dweb", {
    main_url: "dist/user/desktop.worker.cjs",
});
