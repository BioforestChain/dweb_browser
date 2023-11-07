export * from "../common/const.ts";
export const enum OBSERVE {
  State = "observe",
}

export const enum X_EMULATOR_ACTION {
  CLIENT_2_SERVER = "c2s",
  SERVER_2_CLIENT = "s2c",
}

export interface $PlaocConfig {
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
