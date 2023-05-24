export class ServerUrlInfo {
    constructor(
    /**
     * 标准host，是一个站点的key，只要站点过来时用某种我们认可的方式（x-host/user-agent）携带了这个信息，那么我们就依次作为进行网关路由
     */
    host, 
    /**
     * 内部链接，带有特殊的协议头，方便自定义解析器对其进行加工，使用这个链接可以直接等价于在请求中附带 X-Dweb-Host 信息
     */
    internal_origin, 
    /**
     * 相对公网的链接（这里只是相对标准网络访问，当然目前本地只支持localhost链接，所以这里只是针对webview来使用）
     */
    public_origin) {
        Object.defineProperty(this, "host", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: host
        });
        Object.defineProperty(this, "internal_origin", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: internal_origin
        });
        Object.defineProperty(this, "public_origin", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: public_origin
        });
    }
    buildUrl(origin, builder) {
        if (typeof builder === "string") {
            return new URL(builder, origin);
        }
        const url = new URL(origin);
        url.searchParams.set("X-Dweb-Host", this.host);
        return builder(url) ?? url;
    }
    buildPublicUrl(builder) {
        return this.buildUrl(this.public_origin, builder);
    }
    buildInternalUrl(builder) {
        return this.buildUrl(this.internal_origin, builder);
    }
}
export class ServerStartResult {
    constructor(token, urlInfo) {
        Object.defineProperty(this, "token", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: token
        });
        Object.defineProperty(this, "urlInfo", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: urlInfo
        });
    }
}
