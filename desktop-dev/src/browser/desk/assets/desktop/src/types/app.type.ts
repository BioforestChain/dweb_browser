export type { $DeskAppMetaData as $WidgetAppData } from "../../../../types.ts";
import { $AppIconInfo } from "src/components/app-icon/types.ts";
import type { $CommonAppManifest } from '../../../../../../core/types.ts';
import type { $WindowState } from "../../../../types.ts";

export interface $TaskBarState {
  /**
   * 是否聚焦到taskBar
   */
  focus: boolean;
  appId: `${string}.dweb`;
}

export interface $DeskLinkMetaData {
  title:string,
  icon:$AppIconInfo,
  url:string
}


/**
 * 自定义组件名称
 */
export type $WidgetCustomData = {
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



export interface $DeskLinkMetaData extends $CommonAppManifest {
  running: boolean;
  /**
   * 当前进程所拥有的窗口的状态
   */
  winStates: $WindowState[];
}
