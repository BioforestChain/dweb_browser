import { IOS_TIMINGS } from '@bnqkl/framework/animations';
import { $once, createStyle, registryCssProperty } from '@bnqkl/framework/helpers';
import { PromiseOut } from '@bnqkl/util-web/extends-promise-out';
import { BehaviorSubject } from 'rxjs';
import { SimpleKeyboard } from 'simple-keyboard';
import chinese_layout from 'simple-keyboard-layouts/build/layouts/chinese';
import english_layout from 'simple-keyboard-layouts/build/layouts/english';
// import css from 'simple-keyboard/build/css/' assert {type:"text"};
import { $Keyboard, $KeyboardInfo, $KEYBOARD_STATUS } from './types';

/** 虚拟键盘的安装结果 */
const nativeUIInstaller = new PromiseOut<Awaited<ReturnType<typeof setupNativeUI>>>();
/** 安装模拟键盘的UI */
const setupNativeUI = async (root: HTMLElement) => {
  /// 配置样式
  const style = createStyle('plugin-keyboard-ui', root);
  style.innerHTML = await Promise.all([
    fetch('./assets/simple-keyboard/css/index.css').then((res) => res.text()),
    fetch('./assets/simple-keyboard/css/theme.css').then((res) => res.text()),
  ]).then((cssList) => cssList.join('\n'));
  const PLUGIN_KEYBOARD_UI_HEIGHT = registryCssProperty('--plugin-keyboard-ui-height', '<number>', 0, 'false');

  /**
   * 使用button来容纳键盘, 目的是能被捕捉到 focusin 事件捕捉到
   */
  const keyboardEle = document.createElement('button');
  keyboardEle.className = 'simple-keyboard';

  root.appendChild(keyboardEle);
  /**
   * @TODO 这里需要根据输入框的type进行修改
   */
  const layout = navigator.language.startsWith('zh') ? chinese_layout : english_layout;

  const controller = new SimpleKeyboard({
    ...layout,
    theme: ['hg-theme-default', 'hg-layout-default', 'layoutCandidates' in layout ? 'has-candidate' : undefined]
      .filter(Boolean)
      .join(' '),
    onChange(new_value) {
      const inputEle = info.input;
      if (inputEle instanceof HTMLInputElement || inputEle instanceof HTMLTextAreaElement) {
        /// 使用value写入数据, 会袋子 selection 发生改变,所以需要进行矫正
        const selectionStart = inputEle.selectionStart ?? 0;
        const selectionEnd = inputEle.selectionEnd ?? 0;
        const selectionDirection = inputEle.selectionDirection ?? 'forward';

        const old_value = inputEle.value;
        // const old_value_start = old_value.slice(0, selectionStart);
        // const old_value_selected = old_value.slice(selectionStart, selectionEnd);
        const old_value_end = old_value.slice(selectionEnd);
        const new_value_selected = old_value_end.length
          ? new_value.slice(selectionStart, -old_value_end.length)
          : new_value.slice(selectionStart);

        inputEle.value = new_value;

        const newSelectionStart = selectionStart + new_value_selected.length;
        inputEle.setSelectionRange(newSelectionStart, newSelectionStart, selectionDirection);
      } else {
        const selection = getSelection();
        const range = selection?.getRangeAt(0);
        const node = selection?.anchorNode;
        if (selection && range && node instanceof Text) {
          selection.anchorNode;

          /// 使用value写入数据, 会袋子 selection 发生改变,所以需要进行矫正

          const selectionStart = range.startOffset ?? 0;
          const selectionEnd = range.endOffset ?? 0;
          // const selectionDirection = selection.anchorOffset >= selection.focusOffset ? 'forward' : 'backward';

          const old_value = node.textContent ?? '';
          // const old_value_start = old_value.slice(0, selectionStart);
          // const old_value_selected = old_value.slice(selectionStart, selectionEnd);
          const old_value_end = old_value.slice(selectionEnd);
          const new_value_selected = old_value_end.length
            ? new_value.slice(selectionStart, -old_value_end.length)
            : new_value.slice(selectionStart);

          node.textContent = new_value;

          const newSelectionStart = selectionStart + new_value_selected.length;
          selection.setPosition(selection.anchorNode, newSelectionStart);
        }
      }
    },
    mergeDisplay: true,
    display: {
      '{bksp}': '⌫',
      '{enter}': '⏎',
    },
    enableLayoutCandidates: true,
    /**
     * TODO 这里应该监听 hg-candidate-box-list 的宽度来动态设置
     */
    layoutCandidatesPageSize: Math.round((window.innerWidth - 56) / 40),
  });

  // 注入自定义的隐藏按钮
  const hideBtn = document.createElement('button');
  hideBtn.classList.add('hg-hide-btn');
  hideBtn.innerHTML = '❌';
  hideBtn.addEventListener('click', hide);

  keyboardEle.appendChild(hideBtn);

  /// 监听键盘的实际高度
  const ob = new ResizeObserver(() => {
    keyboardEle.style.setProperty(PLUGIN_KEYBOARD_UI_HEIGHT, (info.height = keyboardEle.clientHeight) + 'px');
  });
  ob.observe(keyboardEle);

  /// 监听输入框 聚焦 与 失焦
  {
    const onInput = (e: Event) => {
      const inputEle = e.target as HTMLInputElement;
      const text = inputEle.value;
      if (typeof text === 'string') {
        controller.setInput(text);
      }
    };
    document.addEventListener('focusin', (event) => {
      const ele = event.target;
      /// 在敲击键盘
      if (ele === keyboardEle) {
        return;
      }
      if (
        (ele instanceof HTMLElement && ele.contentEditable == 'true') ||
        // 是多行文本
        ((ele instanceof HTMLTextAreaElement ||
          // 或者是单行文本 (ps: 日期\颜色等输入器看似有键盘, 但在原生中是另外的组件在负责)
          (ele instanceof HTMLInputElement && ele.selectionStart !== null)) &&
          /// 并且没有被禁用
          ele.disabled === false)
      ) {
        /// 弹出键盘
        show();
        /// 绑定事件
        info.input = ele;
        ele.addEventListener('input', onInput);
        /// 绑定输入
        controller.setInput((ele as HTMLInputElement).value ?? ele.textContent);
      }
    });
    document.addEventListener('focusout', (event) => {
      /// 在敲击键盘
      if (event.relatedTarget === keyboardEle) {
        /// 将焦点重新恢复到输入框中
        info.input?.focus();
        return;
      }
      const ele = event.target;
      if (ele === info.input && info.input !== null) {
        /// 解绑事件
        info.input.removeEventListener('input', onInput);
        info.input = null;
        /// 清理输入
        controller.clearInput();
        /// 隐藏键盘
        hide();
      }
    });
    // document.addEventListener('selectionchange', () => {
    //   const inputEle = info.input;
    //   if (inputEle) {
    //     const { selectionStart, selectionEnd, selectionDirection } = inputEle;
    //     info.selection = {
    //       start: selectionStart,
    //       end: selectionEnd,
    //       direction: selectionDirection,
    //     };
    //     console.log('changed', info.selection);
    //   }
    // });
  }

  /// 聚合返回
  const info = {
    keyboard: keyboardEle,
    controller,
    height: keyboardEle.clientHeight,
    input: null as null | HTMLElement,
  };

  return info;
};
window.addEventListener('DOMContentLoaded', () => {
  nativeUIInstaller.resolve(setupNativeUI(document.body));
});

/**
 * 键盘信息监听集合
 */
const getKeyboardInfo$ = $once(() => {
  const info$ = new BehaviorSubject<$KeyboardInfo>({ status: $KEYBOARD_STATUS.DID_HIDE, height: 0 });

  return info$;
});

/** 打开中 */
let opening: Animation | undefined;
/** 关闭中 */
let closing: Animation | undefined;
/** 键盘是否开启 */
let is_open = false;
/** 打开虚拟键盘 */
const show = async () => {
  const nativeUI = nativeUIInstaller.value;
  if (nativeUI === undefined) {
    return;
  }
  /// 去重
  if (is_open) {
    return;
  }
  /// 如果在关闭中, 停止关闭
  if (closing !== undefined) {
    closing.cancel();
    closing = undefined;
  }

  /// 开启
  is_open = true;
  const info$ = getKeyboardInfo$();
  info$.next({ status: $KEYBOARD_STATUS.WILL_SHOW, height: nativeUI.height });

  opening = nativeUI.keyboard.animate(
    [
      { transform: `translateY(${nativeUI.height}px)`, boxShadow: 'none' },
      { transform: `translateY(0px)`, boxShadow: 'var(--box-shadow)' },
    ],
    {
      easing: IOS_TIMINGS.ENTER_EASING as const,
      duration: IOS_TIMINGS.ENTER_DURATION,
      fill: 'forwards',
    }
  );

  const r = new PromiseOut<boolean>();
  opening.onfinish = () => {
    r.resolve(true);
    opening = undefined;
    info$.next({ status: $KEYBOARD_STATUS.DID_SHOW, height: nativeUI.height });
  };
  opening.oncancel = () => {
    r.resolve(false);
    opening = undefined;
  };
  /// 这里不返回
  await r.promise;
};
/** 关闭虚拟键盘 */
const hide = async () => {
  const nativeUI = nativeUIInstaller.value;
  if (nativeUI === undefined) {
    return;
  }
  /// 去重
  if (is_open === false) {
    return;
  }
  /// 如果在开启中, 停止开启
  if (opening !== undefined) {
    opening.cancel();
    opening = undefined;
  }
  /// 关闭
  is_open = false;
  const info$ = getKeyboardInfo$();
  info$.next({ status: $KEYBOARD_STATUS.WILL_HIDE, height: 0 });

  closing = nativeUI.keyboard.animate(
    [
      { transform: `translateY(0px)`, boxShadow: 'var(--box-shadow)' },
      { transform: `translateY(${nativeUI.height}px)`, boxShadow: 'none' },
    ],
    {
      easing: IOS_TIMINGS.LEAVE_EASING as string,
      duration: IOS_TIMINGS.LEAVE_DURATION,
      fill: 'forwards',
    }
  );
  const r = new PromiseOut<boolean>();
  closing.onfinish = () => {
    r.resolve(true);
    closing = undefined;
    info$.next({ status: $KEYBOARD_STATUS.DID_HIDE, height: 0 });
  };
  closing.oncancel = () => {
    r.resolve(false);
    closing = undefined;
  };
  /// 这里不返回
  await r.promise;
};

/** keyboard with moke ui */
const Keyboard: $Keyboard = {
  getKeyboardInfo$,
  show,
  hide,
};

export default Keyboard;
