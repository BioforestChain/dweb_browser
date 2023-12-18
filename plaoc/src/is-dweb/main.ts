export const isDweb = () => {
  const userAgentData = self.navigator.userAgentData;
  // @ts-ignore
  const isPlaoc = self.__native_close_watcher_kit__ !== void 0;

  if(!userAgentData) {
    return isPlaoc;
  }

  const brands = userAgentData.brands.filter(value => {
    return value.brand === "DwebBrowser";
  });

  return brands && brands.length > 0;
};

export const dwebTarget = () => {
  if (isDweb()) {
    const userAgentData = self.navigator.userAgentData;

    if (!userAgentData) {
      return 1.0;
    }
    const brands = userAgentData.brands.filter((value) => {
      return value.brand === "jmm.browser.dweb";
    });

    if (brands && brands.length > 0) {
      return parseFloat(brands[0].version);
    }
  }

  return 1.0;
};

declare global {
  interface Navigator {
    userAgentData: {
      brands: { brand: string; version: string }[];
    } | null;
  }
}
