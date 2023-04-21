import { $once } from '@bnqkl/framework/helpers';
import { NavigationBar, NavigationBarPluginEvents } from '@hugotomazi/capacitor-navigation-bar';
import { BehaviorSubject } from 'rxjs';
import type { NAVIGATION_BAR_COLOR } from './types';

/** 获取导航栏背景色变化的管理器 */
const getNavigationBarColorChange$ = $once(async () => {
  const currentNavigationBarColor = await getNavigationBarColor();
  const navigationBarColor$ = new BehaviorSubject(currentNavigationBarColor);
  NavigationBar.addListener(NavigationBarPluginEvents.COLOR_CHANGE, (colorInfo) => {
    navigationBarColor$.next(colorInfo.color);
  });
  return navigationBarColor$;
});

/** 获取当前导航栏背景色 */
const getNavigationBarColor = async () => {
  const navigationBarColor = await NavigationBar.getColor();
  return navigationBarColor.color;
};

/** 设置导航栏背景色透明（或者恢复原本背景色） */
const setNavigationBarColor = async (color: NAVIGATION_BAR_COLOR) => {
  await NavigationBar.setColor({ color: color });
};

export default {
  getNavigationBarColorChange$,
  getNavigationBarColor,
  setNavigationBarColor,
};
