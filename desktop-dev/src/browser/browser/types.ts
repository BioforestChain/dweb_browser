export interface $Details{
  id: number;
  url: string;
  method: string;
  webContentsId: number;
  webContents: Electron.WebContents;
  frame: Electron.WebFrameMain;
  resourceType: string //  - 可以是 mainFrame， subFrame，stylesheet，script，image，font，object，xhr，ping，cspReport，media，webSocket 或 other。
  referrer: string
  timestamp: number
  uploadData: Electron.UploadData[]
}

export interface $Callback{
  (arg: $Callback.$Response | unknown): void;
}

export declare namespace $Callback{
  export interface $Response{
    cancel?: boolean;
    redirectURL?: string;
  }
}