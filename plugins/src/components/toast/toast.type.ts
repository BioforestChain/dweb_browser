export type ToastDuration = "long" | "short";
export type ToastPosition = "top" | "center" | "bottom";
export interface ToastShowOptions {
  /**
   * Toast 上显示的文本
   *
   * @since 1.0.0
   */
  text: string;

  /**
   * Toast 的持续时间，“短”（2000 毫秒）或“长”（3500 毫秒）
   *
   * @default 'short'
   * @since 1.0.0
   */
  duration?: ToastDuration;

  /**
   * Toast 的位置。
   *
   * 在 Android 12 及更高版本上，所有 toast 都显示在底部。
   *
   * @default 'bottom'
   * @since 1.0.0
   */
  position?: "top" | "center" | "bottom";
}
