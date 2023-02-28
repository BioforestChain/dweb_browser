/**
 * 参考了 @capacitor/browser 的 OpenOptions
 */
export interface $OpenOptions {
  /**
   * The URL to which the browser is opened.
   *
   * @since 1.0.0
   */
  url: string;
  /**
   * Web only: Optional target for browser open. Follows
   * the `target` property for window.open. Defaults
   * to _blank.
   *
   * Ignored on other platforms.
   *
   * @since 1.0.0
   */
  windowName?: string;
  /**
   * A hex color to which the toolbar color is set.
   *
   * @since 1.0.0
   */
  toolbarColor?: string;
  /**
   * iOS only: The presentation style of the browser. Defaults to fullscreen.
   *
   * Ignored on other platforms.
   *
   * @since 1.0.0
   */
  presentationStyle?: 'fullscreen' | 'popover';
  /**
   * iOS only: The width the browser when using presentationStyle 'popover' on iPads.
   *
   * Ignored on other platforms.
   *
   * @since 4.0.0
   */
  width?: number;
  /**
   * iOS only: The height the browser when using presentationStyle 'popover' on iPads.
   *
   * Ignored on other platforms.
   *
   * @since 4.0.0
   */
  height?: number;
}

export type $Browser = typeof import('./browser.dev')['default'];
