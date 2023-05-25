import { CameraDirection } from "../camera/camera.type.ts";

export type CallbackID = string;

export enum SupportedFormat {
  // BEGIN 1D Product
  /**
   * Android only, UPC_A is part of EAN_13 according to Apple docs
   */
  UPC_A = "UPC_A",

  UPC_E = "UPC_E",

  /**
   * Android only
   */
  UPC_EAN_EXTENSION = "UPC_EAN_EXTENSION",

  EAN_8 = "EAN_8",

  EAN_13 = "EAN_13",
  // END 1D Product

  // BEGIN 1D Industrial
  CODE_39 = "CODE_39",

  /**
   * iOS only
   */
  CODE_39_MOD_43 = "CODE_39_MOD_43",

  CODE_93 = "CODE_93",

  CODE_128 = "CODE_128",

  /**
   * Android only
   */
  CODABAR = "CODABAR",

  ITF = "ITF",

  /**
   * iOS only
   */
  ITF_14 = "ITF_14",
  // END 1D Industrial

  // BEGIN 2D
  AZTEC = "AZTEC",

  DATA_MATRIX = "DATA_MATRIX",

  /**
   * Android only
   */
  MAXICODE = "MAXICODE",

  PDF_417 = "PDF_417",

  QR_CODE = "QR_CODE",

  /**
   * Android only
   */
  RSS_14 = "RSS_14",

  /**
   * Android only
   */
  RSS_EXPANDED = "RSS_EXPANDED",
  // END 2D
}

export interface ScanOptions {
  /**
   * This parameter can be used to make the scanner only recognize specific types of barcodes.
   *  If `targetedFormats` is _not specified_ or _left empty_, _all types_ of barcodes will be targeted.
   *
   * @since 1.2.0
   */
  targetedFormats?: SupportedFormat[];
  /**
   * This parameter can be used to set the camera direction.
   *
   * @since 2.1.0
   */
  cameraDirection?: CameraDirection;
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
