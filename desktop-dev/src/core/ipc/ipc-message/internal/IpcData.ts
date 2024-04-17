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
