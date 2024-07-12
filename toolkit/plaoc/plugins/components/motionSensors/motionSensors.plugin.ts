import { bindThis } from "../../helper/bindThis.ts";
import type { $Coder } from "../../util/StateObserver.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { $Axis, $MotionSensorsController } from "./motionSensors.type.ts";

export class MotionSensorsPlugin extends BasePlugin {
  constructor() {
    super("motion-sensors.sys.dweb");
  }

  coder: $Coder<$Axis, $Axis> = {
    decode: (raw) => raw,
    encode: (state) => state,
  };

  /**
   * 拿到加速计传感器控制器
   * @param fps 每秒帧率
   * @Platform android/ios only
   */
  @bindThis
  async startAccelerometer(fps?: number): Promise<$MotionSensorsController> {
    const ws = await this.buildChannel("/observe/accelerometer", {
      search: {
        fps: fps,
      },
    });
    const controller = {
      listen(callback: (axis: $Axis) => void) {
        ws.onmessage = async (ev) => {
          const data = typeof ev.data === "string" ? ev.data : await (ev.data as Blob).text();
          const res = JSON.parse(data);
          callback(res);
        };
      },
      stop() {
        ws.close();
      },
    };
    return controller;
  }

  /**
   * 启动陀螺仪传感器
   * @param fps 每秒帧率
   * @Platform android/ios only
   */
  @bindThis
  async startGyroscope(fps?: number): Promise<$MotionSensorsController> {
    const ws = await this.buildChannel("/observe/gyroscope", {
      search: {
        fps: fps,
      },
    });
    const controller = {
      listen(callback: (axis: $Axis) => void) {
        ws.onmessage = async (ev) => {
          const data = typeof ev.data === "string" ? ev.data : await (ev.data as Blob).text();
          const res = JSON.parse(data);
          callback(res);
        };
      },
      stop() {
        ws.close();
      },
    };
    return controller;
  }
}

export const motionSensorsPlugin = new MotionSensorsPlugin();
