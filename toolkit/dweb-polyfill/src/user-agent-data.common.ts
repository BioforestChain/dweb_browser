import type {} from "./user-agent-data.type.ts";
/// 源码基于 https://gist.github.com/fuweichin/18522d21d3cd947026c2819bda25e0a6 进行ts化

type Brands = {
  brand: string;
  version: string;
  fullVersion?: string;
}[];
if (!navigator.userAgentData) {
  const getClientHints = (navigator: Navigator) => {
    const { userAgent } = navigator;
    let platform = "";
    let platformVersion = "";
    let architecture = "";
    let bitness = "";
    let model = "";
    let uaFullVersion = "";
    const fullVersionList = [];
    let platformInfo = userAgent;
    let found = false;
    const versionInfo = userAgent.replace(/\(([^)]+)\)?/g, ($0, $1) => {
      if (!found) {
        platformInfo = $1;
        found = true;
      }
      return "";
    });
    const items = versionInfo.match(/(\S+)\/(\S+)/g);
    let webview = false;
    // detect mobile
    const mobile = userAgent.indexOf("Mobile") !== -1;
    let m;
    let m2;
    // detect platform
    if ((m = /Windows NT (\d+(\.\d+)*)/.exec(platformInfo)) !== null) {
      platform = "Windows";
      // see https://docs.microsoft.com/en-us/microsoft-edge/web-platform/how-to-detect-win11
      const nt2win: Record<string, string | undefined> = {
        "6.1": "0.1", // win-7
        "6.2": "0.2", // win-8
        "6.3": "0.3", // win-8.1
        "10.0": "10.0", // win-10
        "11.0": "13.0", // win-11
      };
      const ver = nt2win[m[1]];
      if (ver) platformVersion = padVersion(ver, 3);
      if ((m2 = /\b(WOW64|Win64|x64)\b/.exec(platformInfo)) !== null) {
        architecture = "x86";
        bitness = "64";
      }
    } else if ((m = /Android (\d+(\.\d+)*)/.exec(platformInfo)) !== null) {
      platform = "Android";
      platformVersion = padVersion(m[1]);
      if ((m2 = /Linux (\w+)/.exec(navigator.platform)) !== null) {
        if (m2[1]) {
          m2 = parseArch(m2[1]);
          architecture = m2[0];
          bitness = m2[1];
        }
      }
    } else if ((m = /(iPhone|iPod touch); CPU iPhone OS (\d+(_\d+)*)/.exec(platformInfo)) !== null) {
      // see special notes at https://www.whatismybrowser.com/guides/the-latest-user-agent/safari
      platform = "iOS";
      platformVersion = padVersion(m[2].replace(/_/g, "."));
    } else if ((m = /(iPad); CPU OS (\d+(_\d+)*)/.exec(platformInfo)) !== null) {
      platform = "iOS";
      platformVersion = padVersion(m[2].replace(/_/g, "."));
    } else if ((m = /Macintosh; (Intel|\w+) Mac OS X (\d+([_.]\d+)*)/.exec(platformInfo)) !== null) {
      platform = "macOS";
      platformVersion = padVersion(m[2].replace(/_/g, "."));
    } else if ((m = /Linux/.exec(platformInfo)) !== null) {
      platform = "Linux";
      platformVersion = "";
      // TODO
    } else if ((m = /CrOS (\w+) (\d+(\.\d+)*)/.exec(platformInfo)) !== null) {
      platform = "Chrome OS";
      platformVersion = padVersion(m[2]);
      m2 = parseArch(m[1]);
      architecture = m2[0];
      bitness = m2[1];
    }
    if (!platform) {
      platform = "Unknown";
    }
    // detect fullVersionList / brands
    const notABrand = { brand: " Not;A Brand", version: "99.0.0.0" };
    if ((m = /Chrome\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null && navigator.vendor === "Google Inc.") {
      fullVersionList.push({ brand: "Chromium", version: padVersion(m[1], 4) });
      if ((m2 = /(Edge?)\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null) {
        const identBrandMap: Record<string, string | undefined> = {
          Edge: "Microsoft Edge",
          Edg: "Microsoft Edge",
        };
        const brand = identBrandMap[m[1]];
        fullVersionList.push({ brand: brand, version: padVersion(m2[2], 4) });
      } else {
        fullVersionList.push({ brand: "Google Chrome", version: padVersion(m[1], 4) });
      }
      if (/\bwv\b/.exec(platformInfo)) {
        webview = true;
      }
    } else if (
      (m = /AppleWebKit\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null &&
      navigator.vendor === "Apple Computer, Inc."
    ) {
      fullVersionList.push({ brand: "WebKit", version: padVersion(m[1]) });
      if (platform === "iOS" && (m2 = /(CriOS|EdgiOS|FxiOS|Version)\/(\d+(\.\d+)*)/.exec(versionInfo)) != null) {
        const identBrandMap: Record<string, string | undefined> = {
          // no
          CriOS: "Google Chrome",
          EdgiOS: "Microsoft Edge",
          FxiOS: "Mozilla Firefox",
          Version: "Apple Safari",
        };
        const brand = identBrandMap[m[1]];
        fullVersionList.push({ brand, version: padVersion(m[2]) });
        if (items?.findIndex((s) => s.startsWith("Safari/")) === -1) {
          webview = true;
        }
      }
    } else if ((m = /Firefox\/(\d+(\.\d+)*)/.exec(versionInfo)) !== null) {
      fullVersionList.push({ brand: "Firefox", version: padVersion(m[1]) });
    } else if ((m = /(MSIE |rv:)(\d+\.\d+)/.exec(platformInfo)) !== null) {
      fullVersionList.push({ brand: "Internet Explorer", version: padVersion(m[2]) });
    } else {
      fullVersionList.push(notABrand);
    }
    uaFullVersion = (fullVersionList.length > 0 && fullVersionList[fullVersionList.length - 1]?.version) || "";
    const brands = fullVersionList.map((b) => {
      const pos = b.version.indexOf(".");
      const version = pos === -1 ? b.version : b.version.slice(0, pos);
      return { brand: b.brand, version };
    });
    // TODO detect architecture, bitness and model
    return {
      mobile,
      platform,
      brands,
      platformVersion,
      architecture,
      bitness,
      model,
      uaFullVersion,
      fullVersionList,
      webview,
    };
  };
  const parseArch = (arch: string) => {
    switch (arch) {
      case "x86_64":
      case "x64":
        return ["x86", "64"];
      case "x86_32":
      case "x86":
        return ["x86", ""];
      case "armv6l":
      case "armv7l":
      case "armv8l":
        return [arch, ""];
      case "aarch64":
        return ["arm", "64"];
      default:
        return ["", ""];
    }
  };
  const padVersion = (ver: string, minSegs = 3) => {
    const parts = ver.split(".");
    const len = parts.length;
    if (len < minSegs) {
      for (let i = 0, lenToPad = minSegs - len; i < lenToPad; i += 1) {
        parts.push("0");
      }
      return parts.join(".");
    }
    return ver;
  };
  class NavigatorUAData {
    readonly _ch: ReturnType<typeof getClientHints>;
    constructor(navigator: Navigator, brands: Brands) {
      this._ch = getClientHints(navigator);
      this._ch.brands = this._ch.brands.concat(brands.map((b) => ({ brand: b.brand, version: b.version })));
      this._ch.fullVersionList = this._ch.fullVersionList.concat(
        brands.map((b) => ({ brand: b.brand, version: b.fullVersion ?? b.version }))
      );
      Object.defineProperties(this, {
        _ch: { enumerable: false },
      });
    }
    get mobile() {
      return this._ch.mobile;
    }
    get platform() {
      return this._ch.platform;
    }
    get brands() {
      return this._ch.brands;
    }
    // deno-lint-ignore require-await
    async getHighEntropyValues(hints: string[]) {
      if (!Array.isArray(hints)) {
        throw new TypeError("argument hints is not an array");
      }
      const hintSet = new Set(hints);
      const data = this._ch;
      const obj = {
        mobile: data.mobile,
        platform: data.platform,
        brands: data.brands,
        architecture: hintSet.has("architecture") ? data.architecture : undefined,
        bitness: hintSet.has("bitness") ? data.bitness : undefined,
        model: hintSet.has("model") ? data.model : undefined,
        platformVersion: hintSet.has("platformVersion") ? data.platformVersion : undefined,
        uaFullVersion: hintSet.has("uaFullVersion") ? data.uaFullVersion : undefined,
        fullVersionList: hintSet.has("fullVersionList") ? data.fullVersionList : undefined,
      };
      return JSON.parse(JSON.stringify(obj));
    }
    toJSON() {
      const data = this._ch;
      return {
        mobile: data.mobile,
        brands: data.brands,
      };
    }
    static __upsetBrands__(brands: Brands) {
      const userAgentData = new NavigatorUAData(navigator, brands);
      Object.defineProperty(Navigator.prototype, "userAgentData", {
        enumerable: true,
        configurable: true,
        writable: false,
        value: userAgentData,
      });
    }
  }
  Object.assign(self, { NavigatorUAData });
} else {
  Object.assign(NavigatorUAData, {
    __upsetBrands__(brands: Brands) {
      const uaBrands = navigator.userAgentData!.brands;

      const custom_brands = brands.map((b) => ({ brand: b.brand, version: b.version }));
      const custom_fullVersionList = brands.map((b) => ({ brand: b.brand, version: b.fullVersion ?? b.version }));
      delete NavigatorUAData.prototype.brands;
      Object.defineProperty(NavigatorUAData.prototype, "brands", {
        value: uaBrands.concat(custom_brands),
      });
      const getHighEntropyValues_symbol = Symbol.for("getHighEntropyValues");
      NavigatorUAData.prototype[getHighEntropyValues_symbol] = NavigatorUAData.prototype.getHighEntropyValues;
      delete NavigatorUAData.prototype.getHighEntropyValues;
      Object.defineProperty(NavigatorUAData.prototype, "getHighEntropyValues", {
        value: async function getHighEntropyValues(hints: string[]) {
          const result = await this[getHighEntropyValues_symbol](hints);
          result.brands = result.brands.concat(custom_brands);
          if (result.fullVersionList) {
            result.fullVersionList = result.fullVersionList.concat(custom_fullVersionList);
          }
          return result;
        },
      });
    },
  });
}
