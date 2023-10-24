import { appendUrlSearchs } from "../../helper/urlHelper.ts";

export class ServerUrlInfo {
  constructor(
    /**
     * 标准host，是一个站点的key，只要站点过来时用某种我们认可的方式（x-host/user-agent）携带了这个信息，那么我们就依次作为进行网关路由
     */
    readonly host: string,
    /**
     * 内部链接，带有特殊的协议头，方便自定义解析器对其进行加工，使用这个链接可以直接等价于在请求中附带 X-Dweb-Host 信息
     */
    readonly internal_origin: string,
    /**
     * 相对公网的链接（这里只是相对标准网络访问，当然目前本地只支持localhost链接，所以这里只是针对webview来使用）
     */
    readonly public_origin: string,
    /**
     * 在buildUrl的时候，可以携带一些自定义的 Querystring
     *
     * 如果在buildUrl的时候，填写了这个map中同名的参数，会以buildUrl的传入为高优先级
     */
    readonly buildExtQuerys = new Map<string, string>()
  ) {}

  private _buildUrl(origin: string, builder?: $UrlBuilder) {
    if (typeof builder === "string") {
      return appendUrlSearchs(new URL(builder, origin), this.buildExtQuerys);
    }
    const url = new URL(origin);
    return appendUrlSearchs(builder?.(url) ?? url, this.buildExtQuerys);
  }
  buildDwebUrl(builder?: $UrlBuilder) {
    return this._buildUrl(`https://${this.host}`, builder);
  }

  /**
   * 导出基于网关的链接，这里会在 queryString 中强制加上 X-Dweb-Host 信息
   * @param builder
   * @returns
   */
  buildPublicUrl(builder?: $UrlBuilder) {
    return appendUrlSearchs(this._buildUrl(this.public_origin, builder), [["X-Dweb-Host", this.host]]);
  }
  buildPublicHtmlUrl(builder?: $UrlBuilder) {
    return this._buildUrl(
      this._buildUrl(this.public_origin, (url) => {
        url.host = `${this.host.replaceAll(":","-")}.${url.host}`
      }).toString(),
      builder
    );
  }

  /**
   * 导出特殊的平台URL，尽可能保证 pathname 与 query 的信息与  DwebUrl 一致
   * 所以在不同平台上有不同的解决方案：
   * 1. 在 desktop 平台上，我们使用 `http://{subdomain.}{mmid}-{port}.localhost/{pathname}{?query}` 来将 Dweb-Host 信息放在 hostname 里
   *    在 NWJS/Electron 中要实现对 DwebUrl 对捕捉是可行的。但综合考量，因为桌面端性能足够，直接使用网络层来传输就够了。
   * 2. 在 ios 平台上，我们使用 `{subdomain.}{mmid}+{port}:/{pathname}{?query}` 来将 Dweb-Host 信息放在 scheme 里
   *    IOS 虽然可以完整捕捉网络请求，但是 IOS 无法拦截 https/http 请求，虽然可以用 loadSimulatedRequest 来实现网页域名锁定，但是对于后续的请求因为无法拦截，对于使用者来说，是一种心智负担。
   * 3. 在 Android 平台上，我们直接使用 DwebUrl，也就是 `https://{subdomain.}{mmid}:port/{pathname}{?query}`，但因为无法捕捉 request-body，因此只能处理 GET、HEAD 请求。
   *    虽然可以用注入 service-worker 做到捕捉body，但是这样适得其反，延迟与成本过高，因此对于 POST/PUT 等需要携带 body 的其它请求，需要使用 publicUrl。
   * @param builder
   * @returns
   */
  buildInternalUrl(builder?: $UrlBuilder) {
    return this._buildUrl(this.internal_origin, builder);
  }

  buildUrl(usePub = false, builder?: $UrlBuilder) {
    if (usePub) {
      return this.buildPublicUrl(builder);
    } else {
      return this.buildInternalUrl(builder);
    }
  }
  buildHtmlUrl(usePub = false, builder?: $UrlBuilder) {
    if (usePub) {
      return this.buildPublicHtmlUrl(builder);
    } else {
      return this.buildInternalUrl(builder);
    }
  }
}
export class ServerStartResult {
  constructor(readonly token: string, readonly urlInfo: ServerUrlInfo) {}
}

type $UrlBuilder = string | ((url: URL) => URL | void);
