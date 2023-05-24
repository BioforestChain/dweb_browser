"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.fetchBaseExtends = void 0;
const $makeFetchExtends = (exts) => {
    return exts;
};
exports.fetchBaseExtends = $makeFetchExtends({
    async number() {
        const text = await this.text();
        return +text;
    },
    async ok() {
        const response = await this;
        if (response.status >= 400) {
            throw response.statusText || (await response.text());
        }
        else {
            return response;
        }
    },
    async text() {
        const ok = await this.ok();
        return ok.text();
    },
    async binary() {
        const ok = await this.ok();
        return ok.arrayBuffer();
    },
    async boolean() {
        const text = await this.text();
        return text === "true"; // JSON.stringify(true)
    },
    async object() {
        const ok = await this.ok();
        try {
            return (await ok.json());
        }
        catch (err) {
            debugger;
            throw err;
        }
    },
});
