export const isDweb = () => {
  const isDweb = self.navigator.userAgent.includes("Dweb");
  // @ts-ignore
  const isPlaoc = self.__native_close_watcher_kit__ !== void 0;
  return isDweb || isPlaoc;
};

export const dwebTarget = () => {
  if (isDweb()) {
    const brands = self.navigator.userAgentData.brands.filter((value) => {
      value.brand === "jmm.browser.dweb";
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
    };
  }
}
