import type { $BuildRequestInit } from "../../helper/request.ts";

export type $MMID = `${string}.dweb`;

export interface $BuildRequestWithBaseInit extends $BuildRequestInit {
  base?: string;
}

export interface $BuildChannelWithBaseInit extends $BuildRequestWithBaseInit {
  binaryType?: BinaryType;
}
