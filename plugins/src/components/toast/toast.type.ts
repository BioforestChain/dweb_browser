export type Duration = 'long' | 'short';
export type Position = 'top' | 'center' | 'bottom'

export interface IToastPlugin {
  /**
   * Shows a Toast on the screen
   *
   * @since 1.0.0
   */
  show(options: IShowOptions): Promise<Response>;
}

export type Tduration = 'short' | 'long' | undefined;

export interface IShowOptions {
  /**
   * Text to display on the Toast
   *
   * @since 1.0.0
   */
  text: string;

  /**
   * Duration of the Toast, either 'short' (2000ms) or 'long' (3500ms)
   *
   * @default 'short'
   * @since 1.0.0
   */
  duration?: Tduration;

  /**
   * Position of the Toast.
   *
   * On Android 12 and newer all toasts are shown at the bottom.
   *
   * @default 'bottom'
   * @since 1.0.0
   */
  position?: 'top' | 'center' | 'bottom';
}

/**
 * @deprecated Use `ToastShowOptions`.
 * @since 1.0.0
 */
export type IToastShowOptions = IShowOptions;
