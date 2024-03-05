// 模拟状态栏模块-用来提供状态UI的模块
import { MICRO_MODULE_CATEGORY } from "../../core/helper/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";

export class WindowNMM extends NativeMicroModule {
  mmid = "window.sys.dweb" as const;
  name = "desktop";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop];

  _bootstrap = async () => {
    const onFetchHanlder = fetchMatch()
      .get("/maximize", async (event) => {
        return Response.json(true);
      })
      .get("/setStyle", async (event) => {
        return Response.json(true);
      })
      .get("/getState", async (event) => {
        return Response.json(true);
      })
      .get("/setStyle", async (event) => {
        return Response.json(true);
      })
      this.onFetch((event) => onFetchHanlder.run(event)).internalServerError();
  };

  _shutdown() {}
}
