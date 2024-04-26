import { simpleDecoder, simpleEncoder } from "@dweb-browser/helper/encoding.ts";

/**
 * 数据编码格式
 */
export const enum IPC_DATA_ENCODING {
  /** 文本 json html 等 */
  UTF8 = 1 << 1,
  /** 使用文本表示的二进制 */
  BASE64 = 1 << 2,
  /** 二进制 */
  BINARY = 1 << 3,
}

export const $dataToBinary = (data: string | Uint8Array, encoding: IPC_DATA_ENCODING) => {
  switch (encoding) {
    case IPC_DATA_ENCODING.BINARY: {
      return data as Uint8Array;
    }
    case IPC_DATA_ENCODING.BASE64: {
      return simpleEncoder(data as string, "base64");
    }
    case IPC_DATA_ENCODING.UTF8: {
      return simpleEncoder(data as string, "utf8");
    }
  }
  throw new Error(`unknown encoding: ${encoding}`);
};
export const $dataToText = (data: string | Uint8Array, encoding: IPC_DATA_ENCODING) => {
  switch (encoding) {
    case IPC_DATA_ENCODING.BINARY: {
      return simpleDecoder(data as Uint8Array, "utf8");
    }
    case IPC_DATA_ENCODING.BASE64: {
      return simpleDecoder(simpleEncoder(data as string, "base64"), "utf8");
    }
    case IPC_DATA_ENCODING.UTF8: {
      return data as string;
    }
  }
  throw new Error(`unknown encoding: ${encoding}`);
};
