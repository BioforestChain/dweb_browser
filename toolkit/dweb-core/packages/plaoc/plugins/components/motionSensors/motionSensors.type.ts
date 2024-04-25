/**
 * 传感器 x y z 轴数据
 */
export interface $Axis {
  x: number;
  y: number;
  z: number;
}

export interface $MotionSensorsController {
  listen(callback: (position: $Axis) => void): void;
  stop(): void;
}
