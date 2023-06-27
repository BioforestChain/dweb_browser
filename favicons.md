1. 单次获取图标

其中 preference_size 参数为偏好大小，根据项目需求去获取

```js
function getIosIcon(preference_size = 120) {
  const iconLinks = [
    ...document.querySelectorAll(`link[rel~="icon"]`).values(),
  ].map((ele) => {
    return {
      ele,
      rel: ele.getAttribute("rel"),
      sizes: parseInt(ele.getAttribute("sizes")) || 0,
    };
  });

  const href = (
    iconLinks
      /// 优先获取 ios 的指定图标
      .filter((link) => {
        return link.rel === "apple-touch-icon";
      })
      .sort(
        (a, b) =>
          Math.abs(a.size - preference_size) -
          Math.abs(b.size - preference_size)
      )[0] ??
    /// 获取标准网页图标
    iconLinks.findLast(
      (link) => link.rel === "icon" || link.rel === "shortcut icon"
    )
  )?.ele.href;

  if (href) {
    const iconUrl = new URL(href, document.baseURI);
    return iconUrl.href;
  }
  return "";
}
```

2. 轮训获取图标

```js
function watchIosIcon(
  preference_size = 120,
  message_hanlder_name = "favicons",
  loop = 100
) {
  let preIcon = "";
  const getAndPost = () => {
    const curIcon = getIosIcon(watchIosIcon);
    if (curIcon && preIcon !== curIcon) {
      preIcon = curIcon;
      webkit.messageHanlders[message_hanlder_name].postMessage(curIcon);
    }
  };
  getAndPost();
  return setInterval(getAndPost, loop);
}
```
