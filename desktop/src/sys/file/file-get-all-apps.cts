// 获取全部的 app信息
// const fs = require('fs');
const fsPromises = require("fs/promises")
const path = require('path');
const process = require('process')
const chalk = require('chalk')
 
export async function getAllApps(){
    return new Promise(async (resolve, reject) => {
        const appsPath = path.resolve(process.cwd(), "./apps/infos")
        const foldersName: string[]= await fsPromises.readdir(appsPath)
        const appsInfo: $AppMetaData[] = []
        foldersName.forEach(async (folderName: string)=> {
            const metaData= require(path.resolve(appsPath, `./${folderName}`)) as unknown as $AppMetaData;
            appsInfo.push(metaData)
        })
        resolve(appsInfo)
    })
}


export interface $AppMetaData{
    title: string,
    subtitle: string,
    id: string,
    downloadUrl: string;
    icon: string;
    images: string[],
    introduction: string,
    author: string[],
    version: string,
    keywords: string[],
    home: string,
    mainUrl: string,
    staticWebServers: $StaticWebServers[],
    openWebViewList: string[],
    size: string,
    fileHash: string,
    permissions: string,
    plugins: string[],
    releaseDate: string
}

export interface $StaticWebServers{
    root: string;
    entry: string;
    subdomain: string;
    port:number
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





