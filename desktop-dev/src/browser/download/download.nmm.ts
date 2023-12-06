// 模拟状态栏模块-用来提供状态UI的模块
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { ReadableStreamOut } from "../../helper/stream/readableStreamHelper.ts";

export class DownloadNMM extends NativeMicroModule {
  mmid = "download.browser.dweb" as const;
  name = "download";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities];

  _bootstrap = async () => {
    const onFetchHanlder = fetchMatch().duplex("/listen", async (event) => {
      const responseBody = new ReadableStreamOut<Uint8Array>();
      return { body: responseBody.stream };
    });
    this.onFetch((event) => onFetchHanlder.run(event)).internalServerError();
  };

  _shutdown() {}
}
