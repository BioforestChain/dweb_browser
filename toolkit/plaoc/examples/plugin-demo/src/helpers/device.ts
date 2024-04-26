/**
 * 判断是否是 支持 touchstart 事件的设备
 * @returns boolean
 */
export function isTouchDevice() {
  return "ontouchstart" in window;
}
