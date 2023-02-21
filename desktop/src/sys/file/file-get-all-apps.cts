// 获取全部的 app信息
// const fs = require('fs');
const fsPromises = require("fs/promises")
const path = require('path');
const process = require('process')



// E:\project\dweb_browser\desktop\src\sys\file\file-get-all-apps.cts
// E:\project\dweb_browser\desktop\apps
export async function getAllApps(){
    // icon 的位置
    // file/sys/index.html/<link rel="icon" type="image/svg+xml" href="/vite.svg" />

    // html 位置
    // file/sys/index.html

    // 元数据
    // file/boot/link.json
    return new Promise(async (resolve, reject) => {
        const appsPath = path.resolve(process.cwd(), "./apps")
        const foldersName: string[]= await fsPromises.readdir(appsPath)
        console.log('foldersName: ', foldersName)
        const appsInfo: $AppInfo[] = []
        foldersName.forEach(async (folderName: string)=> {
            const metaData= require(path.resolve(appsPath, `./${folderName}/boot/link.json`)) as unknown as $AppInfo
            console.log('metaData: ', metaData)
            appsInfo.push({
                version: metaData.version,
                bfsAppId: metaData.bfsAppId,
                name: metaData.name,
                icon: metaData.icon
            })
        })
        resolve(appsInfo)
    })
}

export interface $AppInfo{
    version: string,
    bfsAppId: string,
    name: string,
    icon: string
}





