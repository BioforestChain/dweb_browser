import { $once } from '@bnqkl/framework/helpers';
import { Keyboard } from '@capacitor/keyboard';
import { BehaviorSubject } from 'rxjs';
import { $KeyboardInfo, $KEYBOARD_STATUS } from './types';

/**
 * 键盘信息监听集合
 */
const getKeyboardInfo$ = $once(() => {
  const info$ = new BehaviorSubject<$KeyboardInfo>({ status: $KEYBOARD_STATUS.DID_HIDE, height: 0 });
  Keyboard.addListener('keyboardWillShow', (info) => {
    info$.next({ status: $KEYBOARD_STATUS.WILL_SHOW, height: info.keyboardHeight });
  });
  Keyboard.addListener('keyboardDidShow', (info) => {
    info$.next({ status: $KEYBOARD_STATUS.DID_SHOW, height: info.keyboardHeight });
  });
  Keyboard.addListener('keyboardWillHide', () => {
    info$.next({ status: $KEYBOARD_STATUS.WILL_HIDE, height: 0 });
  });
  Keyboard.addListener('keyboardDidHide', () => {
    info$.next({ status: $KEYBOARD_STATUS.DID_HIDE, height: 0 });
  });
  return info$;
});

/** 显示键盘 */
const show = () => Keyboard.show();
/** 隐藏键盘 */
const hide = () => Keyboard.hide();

export default {
  getKeyboardInfo$,
  show,
  hide,
};
