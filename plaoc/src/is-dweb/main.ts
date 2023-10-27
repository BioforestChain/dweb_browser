
export const isDweb = () => {
  const isDweb = self.navigator.userAgent.includes("dweb")
  // @ts-ignore
  const isPlaoc = self.__native_close_watcher_kit__ !== void 0
  return isDweb || isPlaoc
}

