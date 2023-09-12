import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { IPC_METHOD, MICRO_MODULE_CATEGORY } from "../../core/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { electronConfig } from "../../helper/electronStore.ts";
import { match } from "../../helper/patternHelper.ts";
import { zq } from "../../helper/zodHelper.ts";

const configLang = "config-lang";

declare global {
  interface ElectronConfig {
    [key: `${typeof configLang}.${string}.dweb`]: string | undefined;
  }
}

export class ConfigNMM extends NativeMicroModule {
  mmid = "config.sys.dweb" as const;
  name = "config Info";
  override short_name = "Device";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service];

  async _bootstrap(context: $BootstrapContext) {
    const query_lang = zq.object({
      lang: zq.string(),
    });

    this.onFetch(async (event) => {
      return match(event)
        .with({ method: IPC_METHOD.GET, pathname: "/setLang" }, () => {
          const { lang } = query_lang(event.searchParams);
          electronConfig.set(`${configLang}.${event.ipc.remote.mmid}`, lang);
          return Response.json(true);
        })
        .with({ method: IPC_METHOD.GET, pathname: "/getLang" }, () => {
          const lang = electronConfig.get(`${configLang}.${event.ipc.remote.mmid}`, () => undefined);
          return new Response(lang)
        })
        .run();
    }).internalServerError();
  }
  _shutdown = async () => {};
}
