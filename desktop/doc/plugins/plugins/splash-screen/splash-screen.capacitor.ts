import { SplashScreen } from '@capacitor/splash-screen';
import type { $HideOptions, $ShowOptions } from './types';

/**
 * 显示启动页
 * @param options
 */
const show = async (options?: $ShowOptions) => {
  await SplashScreen.show(options);
};
/**
 * 隐藏启动页
 * @param options
 */
const hide = async (options?: $HideOptions) => {
  await SplashScreen.hide(options);
};

export default {
  show,
  hide,
};
