import type { MicroModule } from "../../core/micro-module.ts";
import { AdaptersManager } from "../../helper/AdaptersManager.ts";

export type $FetchAdapter = (
  remote: MicroModule,
  parsedUrl: URL,
  requestInit: RequestInit
) => Promise<Response | void>;

export const nativeFetchAdaptersManager = new AdaptersManager<$FetchAdapter>();
