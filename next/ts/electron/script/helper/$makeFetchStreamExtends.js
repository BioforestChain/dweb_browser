"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.fetchStreamExtends = void 0;
const JsonlinesStream_js_1 = require("./JsonlinesStream.js");
const $makeFetchExtends = (exts) => {
    return exts;
};
exports.fetchStreamExtends = $makeFetchExtends({
    /** 将响应的内容解码成 jsonlines 格式 */
    async jsonlines() {
        return (
        // 首先要能拿到数据流
        (await this.stream())
            // 先 解码成 utf8
            .pipeThrough(new TextDecoderStream())
            // 然后交给 jsonlinesStream 来处理
            .pipeThrough(new JsonlinesStream_js_1.JsonlinesStream()));
    },
    /** 获取 Response 的 body 为 ReadableStream */
    stream() {
        return this.then((res) => {
            const stream = res.body;
            if (stream == null) {
                throw new Error(`request ${res.url} could not by stream.`);
            }
            return stream;
        });
    },
});
