

const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld(
  'AAAAA',
  {
    doThing: () => ipcRenderer.send('do-a-thing')
  }
)
