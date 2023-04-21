import { StatusBar, Style } from '@capacitor/status-bar';
import type { $Style } from './types';

/**
 * 设置状态栏背景色
 * @param r 0~255
 * @param g 0~255
 * @param b 0~255
 * @param a 0~1
 */
const setBackgroundColor = async (r: number, g: number, b: number, a: number = 1) => {
  const color = [a * 255, r, g, b].map((v) => (v & 0xff).toString(16).padStart(2, '0')).join('');
  await StatusBar.setBackgroundColor({ color });
};
/**
 * 设置状态栏风格
 *
 * 据观测
 * 在系统主题为 Light 的时候, Default 意味着 白色字体
 * 在系统主题为 Dark 的手, Default 因为这 黑色字体
 * 这兴许与设置有关系, 无论如何, 尽可能避免使用 Default 带来的不确定性
 *
 * @param style
 */
const setStyle = async (style: $Style) => {
  let internalStyle: Style;
  switch (style) {
    case 'Dark':
      internalStyle = Style.Dark;
      break;
    case 'Light':
      internalStyle = Style.Light;
      break;
    default:
      internalStyle = Style.Default;
  }
  await StatusBar.setStyle({ style: internalStyle });
};
/**
 * 获取状态栏风格
 */
const getStyle = async (): Promise<$Style> => {
  switch ((await StatusBar.getInfo()).style) {
    case Style.Dark:
      return 'Dark';
    case Style.Light:
      return 'Light';
    case Style.Default:
      return 'Default';
  }
};
/**
 * 设置状态栏是否覆盖webview
 * @param overlay
 */
const setOverlaysWebView = async (overlay: boolean) => {
  await StatusBar.setOverlaysWebView({ overlay: overlay });
};

export default {
  setBackgroundColor,
  setStyle,
  getStyle,
  setOverlaysWebView,
};
