export const enum OBSERVE {
  State = "observe",
}

export const enum X_PLAOC_QUERY {
  EMULATOR = "X-Plaoc-Emulator",
  PROXY = "X-Plaoc-Proxy",
  API_INTERNAL_URL = "X-Plaoc-Internal-Url",
  API_PUBLIC_URL = "X-Plaoc-Public-Url",
  EXTERNAL_URL = "X-Plaoc-External-Url",
  SESSION_ID = "X-Plaoc-Session-Id",
  GET_CONFIG_URL = "x-Plaoc-Config-Url"
}

export const enum X_EMULATOR_ACTION {
  CLIENT_2_SERVER = "c2s",
  SERVER_2_CLIENT = "s2c",
}
