import { $DWEB_DEEPLINK, $MMID } from "../../helper/types.ts";

export class JmmMetadata {
  constructor(readonly config: $JmmMetadata) {}
}
export interface $JmmMetadata {
  id: $MMID;
  server: $JmmMetadata.$MainServer;
  dweb_deeplinks?: $DWEB_DEEPLINK[]
}

export namespace $JmmMetadata {
  export interface $MainServer {
    root: string;
    entry: string;
  }
}
