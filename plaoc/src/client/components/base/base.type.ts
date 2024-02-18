import { $BuildRequestInit } from "../../helper/request.ts";

export interface $DwebResult {
  success: boolean;
  message: string;
}

export type $MMID = `${string}.dweb`;


export interface $BuildRequestWithBaseInit extends $BuildRequestInit {
  base?: string;
}

export interface $BuildChannelWithBaseInit extends $BuildRequestWithBaseInit {
  binaryType?: BinaryType;
}


