import { $once } from '@bnqkl/framework/helpers';
import { App } from '@capacitor/app';
import { BehaviorSubject, Subject } from 'rxjs';
/** 退出应用 */
const exitApp = () => {
  return App.exitApp();
};

/**
 * 监听物理后退按钮
 */
const getBackButton$ = $once(() => {
  const backButton$ = new Subject<boolean>();
  App.addListener('backButton', (event) => {
    backButton$.next(event.canGoBack);
  });
  return backButton$;
});

/**
 * 监听应用暂停
 */
export const getIsPaused$ = $once(async () => {
  const isPaused$ = new BehaviorSubject(false);
  App.addListener('pause', () => {
    isPaused$.next(true);
  });
  App.addListener('resume', () => {
    isPaused$.next(false);
  });
  return isPaused$;
});

export default {
  exitApp,
  getBackButton$,
  getIsPaused$,
};
