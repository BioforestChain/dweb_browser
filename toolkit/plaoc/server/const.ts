export const enum X_PLAOC_QUERY {
  PROXY = "X-Plaoc-Proxy",
  EXTERNAL_URL = "X-Plaoc-External-Url",
  SESSION_ID = "X-Plaoc-Session-Id",
  GET_CONFIG_URL = "x-Plaoc-Config-Url",
  X_PLAOC_QUERY = "X_PLAOC_QUERY",
}

export const enum OBSERVE {
  State = "observe",
}

export interface $PlaocConfig {
  isClear: boolean;
  usePublicUrl?: boolean;
  defaultConfig: $DefaultConfig;
  redirect: $Redirect[];
  middlewares?: {
    www?: string;
    api?: string;
    external?: string;
  };
}

export interface $DefaultConfig {
  lang: string;
}

export interface $Redirect {
  matchMethod?: string[];
  matchUrl: $MatchUrl;
  to: $To;
}

export interface $MatchUrl {
  pathname?: string;
  search?: string;
}

export interface $To {
  url: string;
  appendHeaders: Record<string, string>;
  removeHeaders: string[];
}
