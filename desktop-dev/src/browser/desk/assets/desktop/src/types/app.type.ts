import type { IpcRenderer } from "electron";
export type { $DesktopAppMetaData } from "../../../../desk/desk.nmm.ts";

declare global {
  interface Window {
    electron: {
      messageSend(...args: unknown[]): void;
      messageOn(callback: $Callback): void;
      on: IpcRenderer["on"];
    };
  }
}

interface $Callback {
  (event: Event, type: string, ...args: unknown[]): void;
}

export type $WidgetMetaData = {
  /**
   * 所属应用名称
   *
   * 会有相关的安全性（未来加入签名验证）
   * 会有相关的限制（基于应用的配置）
   * 会有相关的偏好性（deeplink的偏好）
   */
  appId: string;
  /**
   * 组件名称
   */
  widgetName: string;
  templateHtml: string;
  scopedStyle: string;
  size: $TileSize;
  sizeList: $TileSize[];
};
export interface $TileSize {
  row: $TileSizeType;
  column: $TileSizeType;
}

export type $TileSizeType = number | `${number}%`;
