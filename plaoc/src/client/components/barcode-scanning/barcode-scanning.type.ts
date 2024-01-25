export enum CameraDirection {
  /**
   * 前置摄像头
   * @since 2.0.0
   */
  FRONT = "user",
  /**
   * 后置摄像头
   * @since 2.0.0
   */
  BACK = "environment",
}

export enum SupportedFormat {
  QR_CODE = "QR_CODE",
}

export interface ScanOptions {
  /**
   * 图片偏转角度
   * @since 2.0.0
   */
  rotation?: number;
  /**
   * 选择前后摄像头
   * @since 2.0.0
   */
  direction?: CameraDirection;
  /**
   * video显示宽度
   * @since 2.0.0
   */
  width?: number;
  /**
   * video显示高度
   * @since 2.0.0
   */
  height?: number;
  /**
   * 图片识别类型
   * @since 2.0.0
   */
  formats?: SupportedFormat;
}
export type ScanResult = {
  hasContent: boolean;

  content: string[];

  permission: BarcodeScannerPermission;
};

export enum BarcodeScannerPermission {
  UserReject = "UserReject", //用户拒绝
  UserAgree = "UserAgree", // 用户同意
  UserError = "UserError", // 用户手机版本太低，不支持扫码
}
