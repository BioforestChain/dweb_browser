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
        const appsInfo: $AppInfo[] = []
        foldersName.forEach(async (folderName: string)=> {
            const metaData= require(path.resolve(appsPath, `./${folderName}/boot/link.json`)) as unknown as $AppMetaData
            appsInfo.push({
                folderName: folderName,
                appId: metaData.bfsAppId.toLocaleLowerCase(),
                version: metaData.version,
                bfsAppId: metaData.bfsAppId,
                name: metaData.name,
                icon: metaData.icon
            })
        })
        resolve(appsInfo)
    })
}


export interface $AppMetaData{
    version: string,    // 版本信息
    bfsAppId: string,
    name: string,
    icon: string
}

/**
 * 第三方应用的 app信息
 */
export interface $AppInfo{ 
    folderName: string; // 目录
    appId: string;      // 全部小写后的 bfsAppId
    version: string,    // 版本信息
    bfsAppId: string,
    name: string,
    icon: string
}





