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

  coder: $Coder<string, $Axis> = {
    decode: (raw) => JSON.parse(raw),
    encode: (state) => JSON.stringify(state),
  };

  private response = new JsonlinesStreamResponse(this, this.coder);

  private accelerometerSignal = new Signal<$Callback<[$Axis]>>()
  readonly onAccelerometer = this.accelerometerSignal.listen
  private gyroscopeSignal = new Signal<$Callback<[$Axis]>>()
  readonly onGyroscope = this.gyroscopeSignal.listen

  /**
   * 启动加速计传感器
   * @param interval 采样时间间隔
   */
  @bindThis
  async startAccelerometer(interval?: number) {
    for await(const data of this.response.jsonlines(
      `/startAccelerometer${interval !== undefined ? "?interval=" + interval : ""}`
    )){
      this.accelerometerSignal.emit(data);
    }
  }

  /**
   * 启动陀螺仪传感器
   * @param interval 采样时间间隔
   */
  @bindThis
  async startGyroscope(interval?: number) {
    for await(const data of this.response.jsonlines(
      `/startGyroscope${interval !== undefined ? "?interval=" + interval : ""}`
    )) {
      this.gyroscopeSignal.emit(data);
    }
  }
}

export const motionSensorsPlugin = new MotionSensorsPlugin();
