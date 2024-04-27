declare global {
  interface Navigator {
    userAgentData?: NavigatorUAData;
  }
  interface NavigatorUAData {
    brands: { brand: string; version: string }[];
    mobile?: boolean;
  }
  const NavigatorUAData: {
    new (): NavigatorUAData;
    property: NavigatorUAData;
  };
}
export {};
