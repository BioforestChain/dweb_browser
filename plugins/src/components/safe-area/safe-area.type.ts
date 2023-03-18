import { $Insets, DOMInsets } from "../../util/insets.ts";
import {
  $InsetsRawState,
  $InsetsState,
  $InsetsWritableState,
} from "../base/InsetsPlugin.ts";

export interface $SafeAreaRawState extends $InsetsRawState {
  cutoutInsets: $Insets;
  outerInsets: $Insets;
}
export interface $SafeAreaState extends $InsetsState {
  cutoutInsets: DOMInsets;
  outerInsets: DOMInsets;
}
// deno-lint-ignore no-empty-interface
export interface $SafeAreaWritableState extends $InsetsWritableState {}
