import { bindThis } from "../../helper/bindThis.ts";
import { BaseEvent, Tkit } from "../base/BaseEvent.ts";
import {
  ConnectionStatus,
  ConnectionType,
  NetworkStatusMap,
} from "./network.type.ts";

declare global {
  interface Navigator {
    // deno-lint-ignore no-explicit-any
    connection: any;
    // deno-lint-ignore no-explicit-any
    mozConnection: any;
    // deno-lint-ignore no-explicit-any
    webkitConnection: any;
    onLine: boolean;
  }
}

const _network_status: Tkit = {
  _listeners: {},
  _windowListeners: {},
};

export class NetworkPlugin extends BaseEvent<keyof NetworkStatusMap> {
  constructor() {
    super(_network_status);
    if (typeof window !== "undefined") {
      globalThis.addEventListener("online", this.handleOnline);
      globalThis.addEventListener("offline", this.handleOffline);
    }
  }

  /**
   * 查看网络是否在线
   */
  @bindThis
  onLine(): boolean {
    return navigator.onLine;
  }

  /**
   * 获取网络状态
   * （android only）
   * @returns ConnectionStatus
   */
  @bindThis
  // deno-lint-ignore require-await
  async getStatus(): Promise<ConnectionStatus> {
    if (!globalThis.navigator) {
      throw Error("Browser does not support the Network Information API");
    }

    const connected = window.navigator.onLine;
    const connectionType = translatedConnection();

    const status: ConnectionStatus = {
      connected,
      connectionType: connected ? connectionType : "none",
    };

    return status;
  }

  private handleOnline = () => {
    const connectionType = translatedConnection();

    const status: ConnectionStatus = {
      connected: true,
      connectionType: connectionType,
    };
    this.notifyListeners("onLine", new Event("onLine"));
    this.notifyListeners("change", status);
  };

  private handleOffline = () => {
    const status: ConnectionStatus = {
      connected: false,
      connectionType: "none",
    };
    this.notifyListeners("offLine", new Event("offLine"));
    this.notifyListeners("change", status);
  };
}

export const networkPlugin = new NetworkPlugin();

/**
 * 查看当前的网络连接
 * @returns
 */
function translatedConnection(): ConnectionType {
  const connection =
    window.navigator.connection ||
    window.navigator.mozConnection ||
    window.navigator.webkitConnection;
  let result: ConnectionType = "unknown";
  const type = connection ? connection.type || connection.effectiveType : null;
  if (type && typeof type === "string") {
    switch (type) {
      // possible type values
      case "bluetooth":
      case "cellular":
        result = "cellular";
        break;
      case "none":
        result = "none";
        break;
      case "ethernet":
      case "wifi":
      case "wimax":
        result = "wifi";
        break;
      case "other":
      case "unknown":
        result = "unknown";
        break;
      // possible effectiveType values
      case "slow-2g":
      case "2g":
        result = "2g";
        break;
      case "3g":
        result = "3g";
        break;
      case "4g":
        result = "4g";
        break;
      case "5g":
        result = "5g";
        break;
      case "6g":
        result = "6g";
        break;
      default:
        break;
    }
  }
  return result;
}
