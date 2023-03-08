import type { MicroModule } from "../../core/micro-module.cjs";
import { AdaptersManager } from "../../helper/AdaptersManager.cjs";

export type $FetchAdapter = (
  remote: MicroModule,
  parsedUrl: URL,
  requestInit: RequestInit
) => Promise<Response | void>;

export const nativeFetchAdaptersManager = new AdaptersManager<$FetchAdapter>();


