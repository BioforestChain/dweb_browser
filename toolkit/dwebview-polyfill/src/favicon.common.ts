(() => {
  function getCommonIcon(preference_size = 64) {
    const iconLinks = [...document.head.querySelectorAll(`link[rel*="icon"]`).values()]
      .map((ele) => {
        return {
          ele,
          rel: ele.getAttribute("rel"),
        };
      })
      .filter((link) => {
        return (
          link.rel === "icon" ||
          link.rel === "shortcut icon" ||
          link.rel === "apple-touch-icon" ||
          link.rel === "apple-touch-icon-precomposed"
        );
      })
      .map((link, index) => {
        const sizes = parseInt(link.ele.getAttribute("sizes")) || 32;
        return {
          ...link,
          // 上古时代的图标默认大小是32
          sizes,
          weight: sizes * 100 + index,
        };
      })
      .sort((a, b) => {
        const a_diff = Math.abs(a.sizes - preference_size);
        const b_diff = Math.abs(b.sizes - preference_size);
        /// 和预期大小接近的排前面
        if (a_diff !== b_diff) {
          return a_diff - b_diff;
        }
        /// 权重大的排前面
        return b.weight - a.weight;
      });

    const href =
      (
        iconLinks
          /// 优先不获取 ios 的指定图标
          .filter((link) => {
            return link.rel !== "apple-touch-icon" && link.rel !== "apple-touch-icon-precomposed";
          })[0] ??
        /// 获取标准网页图标
        iconLinks[0]
      )?.ele.href ?? "favicon.ico";

    const iconUrl = new URL(href, document.baseURI);
    return iconUrl.href;
  }
  function watchCommonIcon(preference_size = 64) {
    console.log("start watchCommonIcon");
    let preIcon = "";
    const getAndPost = () => {
      const curIcon = getCommonIcon(preference_size);
      if (curIcon && preIcon !== curIcon) {
        preIcon = curIcon;
        if (typeof __native_favicon_kit__ !== "undefined") {
          __native_favicon_kit__.emitChange(curIcon);
        } else {
          console.log("favicon:", curIcon);
        }
        Object.defineProperty(document, "dwebFaviconHref", { value: curIcon, writable: false });
        return true;
      }
      return false;
    };
    getAndPost();
    const config = { attributes: true, childList: true, subtree: true };
    const observer = new MutationObserver(getAndPost);
    observer.observe(document.head, config);
    return () => observer.disconnect();
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => watchCommonIcon());
  } else {
    watchCommonIcon();
  }
})();
