"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.publicServiceJMM = void 0;
const JmmMetadata_js_1 = require("../../sys/jmm/JmmMetadata.js");
const micro_module_js_js_1 = require("../../sys/jmm/micro-module.js.js");
exports.publicServiceJMM = new micro_module_js_js_1.JsMicroModule(new JmmMetadata_js_1.JmmMetadata({
    id: "public.service.bfs.dweb",
    server: { root: "dweb:///sys", entry: "/bfs_worker/public.service.worker.js" },
}));
