import { regDevGlobal } from '@bnqkl/framework/environments';
import { $once } from '@bnqkl/framework/helpers';
import { Subject } from 'rxjs';
import App from './app.capacitor';

/** 退出应用 */
const exitApp = async () => {
  window.onbeforeunload = (event) => {
    event.returnValue = '确定要退出？';
    return true;
  };
  location.href = location.origin;
};

/**
 * 模拟物理后退按钮
 */
const getBackButton$ = $once(() => {
  const backButton$ = new Subject<boolean>();
  regDevGlobal('clickBackButton', () => {
    backButton$.next(true);
  });
  return backButton$;
});

export default { ...App, exitApp, getBackButton$ };
