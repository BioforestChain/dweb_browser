import "@dweb-browser/polyfill";
/**
 * 判断是不是dweb
 * * @returns boolean
 */
export const isDweb = () => {
  const isDweb = self.navigator.userAgent.includes("Dweb");
  // @ts-ignore
  const isPlaoc = self.__native_close_watcher_kit__ !== void 0;

  if (isDweb || isPlaoc) {
    return true;
  }

  const userAgentData = self.navigator.userAgentData;

  if (!userAgentData) {
    return false;
  }

  const brands = userAgentData.brands.filter((value) => {
    return value.brand === "DwebBrowser";
  });

  return Array.isArray(brands) && brands.length > 0;
};

/**
 * 判断dweb大版本
 * @returns boolean
 */
export const dwebTarget = () => {
  if (isDweb()) {
    const userAgentData = self.navigator.userAgentData;

    if (!userAgentData) {
      return 2.0;
    }
    const brands = userAgentData.brands.filter((value) => {
      return value.brand === "jmm.browser.dweb";
    });

    if (Array.isArray(brands) && brands.length > 0) {
      return parseFloat(brands[0].version);
    }
  }

  return 1.0;
};

/**
 * 判断是否是移动端
 * @returns boolean
 */
export const isMobile = () => {
  // 非dweb环境
  if (!navigator.userAgentData) {
    return true;
  }
  return !!navigator.userAgentData.mobile;
};
