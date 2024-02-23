export interface $GeolocationPosition {
  /**当前状态 */
  state: $GeolocationPositionState;
  /**地理位置坐标包含经纬度 */
  coords: GeolocationCoordinates;
  /**时间戳 */
  timestamp: number;
}
export interface $GeolocationPositionState {
  code: $GeolocationCode;
  message: string | null;
}

export enum $GeolocationCode {
  success = 0,
  permission_denied = 1,
  position_unavailable = 2,
  timeout = 3,
}

export interface $LocationOptions {
  fps?: number;
  precise?: boolean;
  /**最小更新距离(米)(android only) */
  minUpdateDistance?: number;
}

export interface $GeolocationContoller {
  /**不断的监听位置 */
  listen(callback: (position: $GeolocationPosition) => undefined): void;
  /**关闭位置监听 */
  stop(): void;
}
