"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const mime_1 = __importDefault(require("mime"));
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
const stream_1 = require("stream");
const createResolveTo_js_1 = require("../../helper/createResolveTo.js");
const nativeFetch_js_1 = require("./nativeFetch.js");
nativeFetch_js_1.nativeFetchAdaptersManager.append(async (remote, parsedUrl) => {
    /// fetch("file:///*") 匹配
    // console.log('[localFileFetch.cts] parsedUrl ', parsedUrl, " --- ", parsedUrl.protocol === "file:" && parsedUrl.hostname === "")
    if (parsedUrl.protocol === "file:" && parsedUrl.hostname === "") {
        return (async () => {
            try {
                const filepath = createResolveTo_js_1.ROOT + parsedUrl.pathname;
                const stats = await fs_1.default.statSync(filepath);
                if (stats.isDirectory()) {
                    throw stats;
                }
                const ext = path_1.default.extname(filepath);
                return new Response(stream_1.Readable.toWeb(fs_1.default.createReadStream(filepath)), {
                    status: 200,
                    headers: {
                        "Content-Length": stats.size + "",
                        "Content-Type": mime_1.default.getType(ext) || "application/octet-stream",
                    },
                });
            }
            catch (err) {
                return new Response(String(err), { status: 404 });
            }
        })();
    }
}, -1);
