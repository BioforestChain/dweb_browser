export const window_options = {
  autoHideMenuBar: true,
  type: "toolbar", //创建的窗口类型为工具栏窗口
  transparent: true, //设置透明
  alwaysOnTop: true, //窗口是否总是显示在其他窗口之前
  // resizable: false, // 禁止窗口大小缩放
  roundedCorners: true, // 窗口圆角
  frame: false, // 要创建无边框窗口

  vibrancy: "popover", // macos 高斯模糊
  visualEffectState: "active", // macos 失去焦点后仍然高斯模糊

  backgroundMaterial: "mica", // windows 高斯模糊
} satisfies Electron.BrowserWindowConstructorOptions;
