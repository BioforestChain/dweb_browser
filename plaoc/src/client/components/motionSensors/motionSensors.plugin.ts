import { JsonlinesStreamResponse } from "../../helper/JsonlinesStreamHelper.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { $Callback, Signal } from "../../helper/createSignal.ts";
import { $Coder } from "../../util/StateObserver.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $Axis } from "./motionSensors.type.ts";

export class MotionSensorsPlugin extends BasePlugin {
  constructor() {
    super("motion-sensors.sys.dweb");
  }

  coder: $Coder<$Axis, $Axis> = {
    decode: (raw) => raw,
    encode: (state) => state,
  };

  private response = new JsonlinesStreamResponse(this, this.coder);

  private accelerometerSignal = new Signal<$Callback<[$Axis]>>();
  readonly onAccelerometer = this.accelerometerSignal.listen;
  private gyroscopeSignal = new Signal<$Callback<[$Axis]>>();
  readonly onGyroscope = this.gyroscopeSignal.listen;

  /**
   * 启动加速计传感器
   * @param fps 每秒帧率
   */
  @bindThis
  async startAccelerometer(fps?: number) {
    for await (const data of this.response.jsonlines("/observe/accelerometer", {
      searchParams: new URLSearchParams(fps !== undefined ? "?fps=" + fps : ""),
    })) {
      console.log(data);
      this.accelerometerSignal.emit(data);
    }
  }

  /**
   * 启动陀螺仪传感器
   * @param fps 每秒帧率
   */
  @bindThis
  async startGyroscope(fps?: number) {
    for await (const data of this.response.jsonlines("/observe/gyroscope", {
      searchParams: new URLSearchParams(fps !== undefined ? "?fps=" + fps : ""),
    })) {
      this.gyroscopeSignal.emit(data);
    }
  }
}

export const motionSensorsPlugin = new MotionSensorsPlugin();
