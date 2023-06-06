export interface $AppMetaData {
  title: string;
  short_name: string;
  id: string;
  bundle_url: string;
  bundle_hash: string;
  bundle_size: number;
  icon: string;
  images: string[];
  description: string;
  author: string[];
  version: string;
  categories: string[];
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
  release_date: string;
}

export interface $StaticWebServers {
  root: string;
  entry: string;
  subdomain: string;
  port: number;
}
