export interface NetworkStatusMap {
  change: ConnectionStatus;
  onLine: Event;
  offLine: Event;
}

/**
 * Represents the state and type of the network connection.
 *
 * @since 1.0.0
 */
export interface ConnectionStatus {
  /**
   * 是否存在网络连接。
   *
   * @since 1.0.0
   */
  connected: boolean;

  /**
   * 当前使用的网络连接类型。
   *
   * 如果没有活动的网络连接，`connectionType` 将为 `'none'`。
   */
  connectionType: ConnectionType;
}

/**
 * The type of network connection that a device might have.
 *
 * @since 1.0.0
 */
export type ConnectionType =
  | "wifi"
  | "cellular" // 蜂窝网络
  | "2g"
  | "3g"
  | "4g"
  | "5g"
  | "6g"
  | "none"
  | "unknown";
