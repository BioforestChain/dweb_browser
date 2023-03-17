import { $Insets, DOMInsets } from "../../util/insets.ts";

export interface $SafeAreaRawState {
  cutoutInsets: $Insets;
  overlay: boolean;
  outerInsets: $Insets;
  innerInsets: $Insets;
}
export interface $SafeAreaState {
  cutoutInsets: DOMInsets;
  overlay: boolean;
  outerInsets: DOMInsets;
  innerInsets: DOMInsets;
}
export interface $SafeAreaWritableState {
  overlay: boolean;
}
