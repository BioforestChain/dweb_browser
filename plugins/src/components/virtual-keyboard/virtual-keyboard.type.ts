import {
  $InsetsRawState,
  $InsetsState,
  $InsetsWritableState,
} from "../base/InsetsPlugin.ts";

export interface $VirtualKeyboardRawState extends $InsetsRawState {
  visible: boolean;
}
export interface $VirtualKeyboardState extends $InsetsState {
  visible: boolean;
}
export interface $VirtualKeyboardWritableState extends $InsetsWritableState {
  visible: boolean;
}
