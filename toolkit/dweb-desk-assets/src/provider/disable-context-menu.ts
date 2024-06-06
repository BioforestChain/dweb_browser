/// 禁用系统菜单，但是出了 自定义标签 和 输入框
window.oncontextmenu = (event) => {
  // @ts-ignore
  const tagName = event.target?.tagName.toLowserCase() ?? "";
  if (tagName === "input" || tagName === "textarea" || tagName.startsWith("browser.")) {
    return;
  }
  event.preventDefault();
};
export {};
