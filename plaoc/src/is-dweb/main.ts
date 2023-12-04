export const isDweb = () => {
  const isDweb = self.navigator.userAgent.includes("Dweb");
  // @ts-ignore
  const isPlaoc = self.__native_close_watcher_kit__ !== void 0;
  return isDweb || isPlaoc;
};

export const dwebTarget = () => {
  if (isDweb()) {
    const matches = self.navigator.userAgent.match(/Dweb\/([\d\.]+)/);
    
    if (matches && matches.length > 1) {
      return parseFloat(matches[1]);
    }
  }

  return 1.0;
};
