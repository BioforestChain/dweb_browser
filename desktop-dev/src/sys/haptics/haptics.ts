// 模拟状态栏模块-用来提供状态UI的模块
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";

export class HapticsNMM extends NativeMicroModule {
  mmid = "haptics.sys.dweb" as const;
  name = "haptics";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities];

  _bootstrap = async () => {
    const onFetchHanlder = fetchMatch()
      .get("/impactLight", async (event) => {
        return Response.json(true);
      })
      .get("/notification", async (event) => {
        return Response.json(true);
      })
      .get("/vibrateClick", async (event) => {
        return Response.json(true);
      })
      .get("/vibrateDisabled", async (event) => {
        return Response.json(true);
      })
      .get("/vibrateDoubleClick", async (event) => {
        return Response.json(true);
      })
      .get("/vibrateHeavyClick", async (event) => {
        return Response.json(true);
      })
      .get("/vibrateTick", async (event) => {
        return Response.json(true);
      })
      this.onFetch((event) => onFetchHanlder.run(event)).internalServerError();
  };

  _shutdown() {}
}
