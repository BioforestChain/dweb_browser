export type $Rect = {
  x: number;
  y: number;
  width: number;
  height: number;
};
export const rectToDom = (rect: $Rect) =>
  new DOMRect(rect.x, rect.y, rect.width, rect.height);
export const domRectToJson = (domRect: DOMRect): $Rect => ({
  x: domRect.x,
  y: domRect.y,
  width: domRect.width,
  height: domRect.height,
});
