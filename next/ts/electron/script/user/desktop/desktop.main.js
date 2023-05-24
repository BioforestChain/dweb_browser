"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.desktopJmm = void 0;
const JmmMetadata_js_1 = require("../../sys/jmm/JmmMetadata.js");
const micro_module_js_js_1 = require("../../sys/jmm/micro-module.js.js");
exports.desktopJmm = new micro_module_js_js_1.JsMicroModule(new JmmMetadata_js_1.JmmMetadata({
    id: "desktop.sys.dweb",
    server: { root: "file:///bundle", entry: "/desktop.worker.js" },
}));
