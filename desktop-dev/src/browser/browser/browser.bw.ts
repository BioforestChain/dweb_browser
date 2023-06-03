// browserWindow 
import Electron from "electron";

export function createBrowserWindow(): $BW{
  const { x, y, width, height} = getInitSize()
  // 放到页面居中的位置
  const options = {
    x: x, 
    y: y,
    width: width,
    height: height,
    movable: true,
    webPreferences: {
      devTools: true,
      webSecurity: false,
      safeDialogs: true,
    },
  }
  const bw = new Electron.BrowserWindow(options) as Electron.BrowserWindow;

  Reflect.set(
    bw, 
    "getTitleBarHeight",
    getTitleBarHeight
  )
  return bw;
}
 


function getInitSize(){
  const size = Electron.screen.getPrimaryDisplay().size
  const width = parseInt(size.width * 0.8 + "");
  const height = parseInt(size.height * 0.8 + "")
  const x = 0;
  const y = (size.height - height) / 2
  return {width, height, x, y}
}


function getTitleBarHeight(this: Electron.BrowserWindow){
  return this.getBounds().height - this.getContentBounds().height;
}

export type $BW = Electron.BrowserWindow & $ExtendsBrowserWindow;

export interface $ExtendsBrowserWindow{
  getTitleBarHeight(): number;
}