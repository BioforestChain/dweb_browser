import type { GeolocationController } from "./geolocation.plugin.ts";

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
  precise?: boolean;
  /**最小更新距离(米)(android only) */
  minDistance?: number;
}

export type $GeolocationController = GeolocationController;
