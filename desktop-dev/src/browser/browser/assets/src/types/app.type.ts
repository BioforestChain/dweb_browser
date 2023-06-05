export interface $AppMetaData {
  title: string;
  short_name: string;
  id: string;
  bundleUrl: string;
  bundleHash: string;
  bundleSize: number;
  icon: string;
  images: string[];
  description: string;
  author: string[];
  version: string;
  keywords: string[];
  home: string;
  mainUrl: string;
  server: {
    root: string;
    entry: string;
  };
  splashScreen: { entry: string };
  staticWebServers: $StaticWebServers[];
  openWebViewList: [];
  permissions: string[];
  plugins: string[];
  releaseDate: string;
}

export interface $StaticWebServers {
  root: string;
  entry: string;
  subdomain: string;
  port: number;
}
