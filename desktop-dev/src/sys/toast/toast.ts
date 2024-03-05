// 模拟状态栏模块-用来提供状态UI的模块
import { MICRO_MODULE_CATEGORY } from "../../core/helper/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";

export class ToastNMM extends NativeMicroModule {
  mmid = "toast.sys.dweb" as const;
  name = "toast";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities];

  _bootstrap = async () => {
    const onFetchHanlder = fetchMatch()
      .get("/show", async (event) => {
        return Response.json(true);
      })
      this.onFetch((event) => onFetchHanlder.run(event)).internalServerError();
  };

  _shutdown() {}
}
