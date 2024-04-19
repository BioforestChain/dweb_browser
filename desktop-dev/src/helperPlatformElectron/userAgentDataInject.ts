
export class NativeWindow {
  static brands: $NativeWindowUserAgentBrandData[] = [];
}

export interface $NativeWindowUserAgentBrandData {
  brand: string;
  version: string;
}

export const setUserAgentData = (webContents: Electron.WebContents) => {
  const brands: $NativeWindowUserAgentBrandData[] = [];
  NativeWindow.brands.forEach((value) => {
    brands.push({
      brand: value.brand,
      version: value.version.includes(".") ? value.version.split(".")[0] : value.version,
    });
  });
  const appVersion = Electron.app.getVersion();
  brands.push({ brand: "DwebBrowser", version: appVersion.includes(".") ? appVersion.split(".")[0] : appVersion });

  webContents.executeJavaScript(`
    const originalBrands = navigator.userAgentData.brands;
    Object.defineProperty(navigator.userAgentData.__proto__, 'brands', {
      configurable: true,
      enumerable: true,
      get: () => [...originalBrands, ...${JSON.stringify(brands)}],
    });
  `);
};