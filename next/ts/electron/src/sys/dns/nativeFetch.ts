import type { MicroModule } from "../../core/micro-module.js";
import { AdaptersManager } from "../../helper/AdaptersManager.js";

export type $FetchAdapter = (
  remote: MicroModule,
  parsedUrl: URL,
  requestInit: RequestInit
) => Promise<Response | void>;

export const nativeFetchAdaptersManager = new AdaptersManager<$FetchAdapter>();


