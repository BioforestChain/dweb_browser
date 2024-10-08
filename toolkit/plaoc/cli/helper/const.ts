import type { $JmmAppInstallManifest } from "@plaoc/server/middlewares";
export type { $JmmAppInstallManifest, $MMID } from "@plaoc/server/middlewares";

/**plaoc bundle */
export type $BundleOptions = $MetadataJsonGeneratorOptions & {
  clear?: boolean;
  out: string;
};

/**plaoc serve */
export type $ServeOptions = $MetadataJsonGeneratorOptions & {
  port: string;
};

/**plaoc live
 * @alias port plaoc 安装地址
 * @alias staticPort 监听静态文件地址
 */
export type $LiveOptions = $MetadataJsonGeneratorOptions & {
  port: string;
  staticPort: string;
};

export type $MetadataJsonGeneratorOptions = {
  webPublic: string;
  version?: string;
  id?: string;
  configDir?: string;
  webServer?: string;
};

export const defaultMetadata: $JmmAppInstallManifest = {
  id: "app-test.plaoc.dweb",
  server: {
    root: "/usr",
    entry: "/server/plaoc.server.js",
  },
  minTarget: 3,
  maxTarget: 3,
  name: "Demo",
  short_name: "plaoc demo",
  description: "This is WebApp bundle by plaoc",
  logo: `data:image/svg+xml;base64,${btoa(
    `<?xml version="1.0" standalone="no"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg t="1685453163956" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="3341" data-spm-anchor-id="a313x.7781069.0.i0" xmlns:xlink="http://www.w3.org/1999/xlink" width="200" height="200"><path d="M512 32c-265.094 0-480 214.925-480 480s214.906 480 480 480 480-214.925 480-480-214.906-480-480-480zM512 944.998c-239.146 0-432.998-193.853-432.998-432.998s193.853-432.998 432.998-432.998 432.998 193.853 432.998 432.998-193.853 432.998-432.998 432.998zM773.581 530.605l0.154-8.871h160.234c0.211-3.206 0.413-6.442 0.413-9.725s-0.202-6.48-0.413-9.725h-160.234l-0.154-8.832c-1.277-69.216-9.619-135.322-24.826-196.435l-1.977-8.064 7.814-2.659c38.314-13.085 67.277-27.197 86.726-38.045-4.205-5.203-8.525-10.301-12.941-15.322-23.203 12.729-49.411 24.115-77.971 34.032l-8.851 3.043-2.736-8.957c-16.128-53.049-37.037-99.36-62.237-137.597-11.289-4.819-22.857-9.101-34.579-12.931 31.277 39.859 57.706 93.341 77.232 156.652l2.736 8.871-8.957 2.468c-55.632 15.36-116.88 23.846-182.074 25.277l-9.245 0.192v-213.908c-3.188-0.231-6.412-0.461-9.696-0.461s-6.489 0.231-9.715 0.461v213.917l-9.245-0.192c-65.002-1.421-126.24-9.994-182.026-25.421l-8.966-2.468 2.736-8.88c19.546-63.283 45.974-116.678 77.194-156.461-11.741 3.792-23.289 8.064-34.598 12.892-25.143 38.16-46.032 84.384-62.16 137.405l-2.755 8.995-8.842-3.091c-28.474-9.869-54.643-21.293-77.923-33.994-4.454 5.088-8.823 10.224-13.037 15.475 19.44 10.838 48.413 24.96 86.717 38.045l7.814 2.659-1.968 8.064c-15.188 61.075-23.549 127.181-24.845 196.435l-0.173 8.832h-160.176c-0.221 3.235-0.413 6.432-0.413 9.715s0.202 6.518 0.413 9.725h160.205l0.173 8.871c1.315 69.869 9.802 136.551 25.219 198.134l2.045 8.064-7.872 2.659c-37.872 12.922-66.682 26.851-86.054 37.584 4.205 5.212 8.544 10.301 12.979 15.322 23.059-12.499 49.075-23.808 77.395-33.6l8.842-3.052 2.717 8.909c16.080 52.397 36.816 98.131 61.728 135.936 11.309 4.781 22.857 9.101 34.608 12.892-30.989-39.437-57.216-92.294-76.685-154.886l-2.794-8.909 9.014-2.468c55.459-15.235 116.534-23.692 181.497-25.123l9.245-0.192v212.103c3.226 0.202 6.432 0.394 9.715 0.394s6.509-0.192 9.715-0.394v-212.103l9.245 0.192c64.762 1.421 125.808 9.917 181.459 25.277l8.976 2.468-2.774 8.909c-19.469 62.506-45.706 115.334-76.617 154.733 11.741-3.792 23.309-8.112 34.608-12.892 24.874-37.805 45.609-83.463 61.671-135.744l2.736-8.957 8.842 3.082c28.253 9.763 54.211 21.024 77.299 33.532 4.522-5.049 8.88-10.186 13.094-15.428-19.354-10.732-48.125-24.653-86.035-37.584l-7.863-2.659 2.016-8.064c15.417-61.574 23.904-128.256 25.2-198.125zM502.285 702.416l-8.851 0.202c-67.181 1.383-130.195 10.023-187.306 25.651l-8.889 2.468-2.246-8.909c-15.331-60.384-23.779-124.589-25.143-190.877l-0.173-9.216h232.608v180.682zM502.285 502.275h-232.608l0.173-9.216c1.373-65.664 9.677-129.293 24.691-189.149l2.246-8.909 8.889 2.429c57.254 15.783 120.423 24.471 187.766 25.814l8.851 0.231v178.8zM521.715 323.475l8.851-0.231c67.344-1.354 130.512-10.032 187.766-25.814l8.889-2.429 2.246 8.909c15.024 59.846 23.337 123.475 24.663 189.149l0.202 9.216h-232.608v-178.8zM729.008 721.828l-2.266 8.909-8.871-2.468c-57.111-15.629-120.125-24.269-187.306-25.661l-8.851-0.202v-180.682h232.608l-0.202 9.216c-1.363 66.326-9.802 130.579-25.114 190.886z" fill="#272636" p-id="3342"></path></svg>`
  )}`,
  bundle_url: "./www.zip",
  bundle_hash: "sha256:",
  bundle_size: 0,
  /**
   * 格式为 `hex:{signature}`
   * */
  bundle_signature: "",
  /**
   * 该链接必须使用和app-id同域名的网站链接，
   * 请求回来是一个“算法+公钥地址”的格式 "{algorithm}:hex;{publicKey}"，
   * 比如说 `rsa-sha256:hex;2...1`
   * 该字段将会用于验证应用持有者的身份
   *  */
  public_key_url: "",
  release_date: new Date().toString(),
  change_log: "",
  images: [],
  author: [],
  version: "1.0.0",
  categories: [],
  languages: [],
  homepage_url: "https://dweb-browser.org/plaoc",
  plugins: [],
  permissions: [],
  dir: "ltr",
  lang: "",
  icons: [],
  screenshots: [],
  display: "fullscreen",
  orientation: "any",
  theme_color: "",
  background_color: "",
  shortcuts: [],
};
