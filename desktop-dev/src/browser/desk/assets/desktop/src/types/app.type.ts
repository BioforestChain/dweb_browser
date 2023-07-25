export type { $DeskAppMetaData as $WidgetAppData } from "../../../../desk.nmm.ts";

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
