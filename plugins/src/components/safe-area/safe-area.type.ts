import { $Rect } from "../../util/rect.ts";

export interface $SafeAreaRawState {
  cutoutRect: $Rect;
  overlay: boolean;
  boundingOuterRect: $Rect;
  boundingInnerRect: $Rect;
}
export interface $SafeAreaState {
  cutoutRect: DOMRect;
  overlay: boolean;
  boundingOuterRect: DOMRect;
  boundingInnerRect: DOMRect;
}
