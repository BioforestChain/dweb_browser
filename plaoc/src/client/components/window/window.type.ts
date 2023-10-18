export type $WindowRawState = {
  topBarOverlay: boolean;
  topBarContentColor: $WindowStyleColor;
  topBarBackgroundColor: $WindowStyleColor;
  bottomBarOverlay: boolean;
  bottomBarContentColor: $WindowStyleColor;
  bottomBarBackgroundColor: $WindowStyleColor;
  themeColor: $WindowStyleColor;
};

export type $DisplayState = {
  /**
   * 屏幕高度
   */
  height: number;
  /**
   * 屏幕宽度
   */
  width: number;
  /**
   * 键盘外边界位置大小
   */
  imeBoundingRect: $Rect;
};

export type $Rect = {
  width: number;
  height: number;
  x: number;
  y: number;
};

export type $WindowState = $WindowRawState;
export type $WindowStyleColor = "auto" | `#${string}`;

export interface $AlertOptions {
  title: string;
  message: string;
  iconUrl?: string;
  iconAlt?: string;
  confirmText?: string;
  dismissText?: string;
}

export interface $AlertModal extends $Modal, $AlertOptions {
  type: "alert";
  confirmCallbackUrl?: string;
  dismissCallbackUrl?: string;
}

export interface $BottomSheetsModal extends $Modal {
  type: "bottom-sheets";
  dismissCallbackUrl?: string;
}

export interface $Modal {
  modalId: string;
  closeTip?: string;
  renderId: string;
}
