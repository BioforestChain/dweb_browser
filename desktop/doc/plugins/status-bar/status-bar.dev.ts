import { createStyle, css, html, isIOS } from '@bnqkl/framework/helpers';
import type { $StatusBar, $Style } from './types';

/**
 * 设置状态栏背景色
 * @param r 0~255
 * @param g 0~255
 * @param b 0~255
 * @param a 0~1
 */
const setBackgroundColor = async (r: number, g: number, b: number, a: number = 1) => {
  /**
   * @TODO 在网页模拟器中进行模拟
   */
  console.log('setBackgroundColor', `rgba(${r},${g},${b},${a})`);
};
/** 状态栏风格 缓存值 */
let _style: $Style = 'Default';
/**
 * 设置状态栏风格
 * @param style
 */
const setStyle = async (style: $Style) => {
  _style = style;
  console.log('setStyle', style);
};
/**
 * 获取状态栏风格
 * @returns
 */
const getStyle = async () => {
  return _style;
};

/**
 * 设置状态栏是否覆盖webview
 * @param overlay
 */
const setOverlaysWebView = async (overlay: boolean) => {
  console.log('setOverlaysWebView', overlay);
};

/**
 * StatusBar API
 */
const StatusBar: $StatusBar = {
  setBackgroundColor,
  setStyle,
  getStyle,
  setOverlaysWebView,
};
export default StatusBar;

/**
 * 开发者模式下, 显示模拟的原生UI ,包括 status-bar 和 navigation-bar
 * @TODO 未来使用 Plaoc-desktop-dev 来实现原生的模拟
 */
const setupNativeUI = (root: HTMLElement) => {
  const is_ios = isIOS();
  const styleEle = createStyle('plugin-status-bar-ui', root);

  const statusbar = document.createElement('div');
  if (is_ios) {
    statusbar.classList.add('ios');
  }
  const setStatusBarStyle = (style: $Style) => {
    if (style === 'Default') {
      /** @TODO addEventListener('change') */
      style = matchMedia('(prefers-color-scheme: dark)').matches ? 'Dark' : 'Light';
    }
    const className = style.toLowerCase();
    if (statusbar.classList.contains(className)) {
      return;
    }
    statusbar.classList.remove('dark', 'light');
    statusbar.classList.add(className);
  };
  StatusBar.getStyle().then(setStatusBarStyle);
  const setStyle = StatusBar.setStyle;
  StatusBar.setStyle = (style) => {
    setStatusBarStyle(style);
    return setStyle(style);
  };

  statusbar.innerHTML = html`
    <div class="left">
      <span class="time"></span>
    </div>
    ${is_ios
      ? html`
          <div class="mask"></div>
        `
      : ``}
    <div class="right">
      <span class="icon cellular-status"></span>
      <span class="icon wifi-status"></span>
      <span class="icon battery"></span>
    </div>
  `;
  statusbar.id = 'status-bar';
  const time = statusbar.querySelector('.time') as HTMLDivElement;
  const updateTime = () => setTime(new Date());
  const setTime = (d: Date) => {
    const pl = (v: number) => v.toString().padStart(2, '0');
    return (time.innerHTML = `${pl(d.getHours())}:${pl(d.getMinutes())}:${pl(d.getSeconds())}`);
  };
  updateTime();
  setInterval(updateTime, 1000);
  root.appendChild(statusbar);

  const navigationbar = document.createElement('div');
  navigationbar.id = 'navigation-bar';
  if (is_ios) {
    navigationbar.classList.add('ios');
  }
  navigationbar.innerHTML = html`<button class="nav-controller-bar"></div>`;
  root.appendChild(navigationbar);

  styleEle.innerHTML += css`
    #status-bar {
      position: fixed;
      z-index: 10000;
      width: 100%;
      height: var(--env-safe-area-inset-top);
      top: 0;
      left: 0;

      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;

      font-size: 12px;
      padding: 0 1em;
    }
    #status-bar.dark {
      background-color: rgba(0, 0, 0, 0.3);
      color: #fff;
    }
    #status-bar.light {
      background-color: rgba(255, 255, 255, 0.3);
      color: #000;
    }
    #status-bar > * {
      flex: 1;

      display: inline-flex;
      gap: 0.5em;
      flex-direction: row;
      justify-content: center;
      align-items: center;
    }

    #status-bar .mask {
      flex-basis: 24%;
      height: 80%;
      background: #000;
      align-self: flex-start;
      --round: calc(var(--env-safe-area-inset-top) * 0.5);
      border-radius: 0 0 var(--round) var(--round);
      position: relative;
    }

    #status-bar .mask::before,
    #status-bar .mask::after {
      position: absolute;
      top: 0;
      --out-round: calc(var(--env-safe-area-inset-top) * 0.5);
      display: inline-block;
      content: ' ';
      background: transparent;
      height: var(--out-round);
      width: var(--out-round);
      border-radius: 100%;
      box-shadow: 0 0 0 var(--out-round) #000;
    }
    #status-bar .mask::before {
      left: calc(-1 * var(--out-round));
      clip-path: inset(0% 0% 50% 50%);
    }
    #status-bar .mask::after {
      right: calc(-1 * var(--out-round));
      clip-path: inset(0% 50% 50% 0);
    }

    #status-bar .right .icon {
      display: inline-block;
      height: 1em;
      width: 1em;

      border-radius: 100%;
      overflow: hidden;
      font-size: 12px;
    }
    #status-bar .right .cellular-status {
      background: #f44336;
    }
    #status-bar .right .wifi-status {
      background: #f57f17;
    }
    #status-bar .right .battery {
      background: #4caf50;
    }
    #navigation-bar {
      position: fixed;
      z-index: 10000;
      width: 100%;
      height: var(--env-safe-area-inset-bottom);
      bottom: 0;
      left: 0;
      display: grid;
      place-items: center;
    }
    #navigation-bar .nav-controller-bar {
      width: 38%;
      min-height: 3px;
      border-radius: 1em;
      background-color: #000000;
    }
    :root {
      background: #000;
    }
    body {
      background: #fff;
      clip-path: inset(
        0% 0% 0% 0% round var(--env-safe-area-inset-top) var(--env-safe-area-inset-top)
          var(--env-safe-area-inset-bottom) var(--env-safe-area-inset-bottom)
      );
    }
    .bnqkl-app {
      border-radius: var(--env-safe-area-inset-top) var(--env-safe-area-inset-top) var(--env-safe-area-inset-bottom)
        var(--env-safe-area-inset-bottom);
      overflow: hidden;
    }
  `;

  // IOS-UI
  if (is_ios) {
    styleEle.innerHTML += css`
      #status-bar.ios {
        font-size: 16px;
        padding: 0 1.2em;
        font-weight: 500;
      }
      #navigation-bar.ios .nav-controller-bar {
        width: 35.5%;
        min-height: 5px;
        margin-top: 10px;
        border-radius: 1em;
        background-color: #000000;
      }
    `;
  }
  return {
    setTime,
    setStatusBarStyle,
  };
};

window.addEventListener('DOMContentLoaded', () => {
  setupNativeUI(document.body);
});
