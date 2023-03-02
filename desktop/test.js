  
const fsPromises = require("node:fs/promises");
const fs = require('node:fs')
const path  = require("path");
const request = require('request');
const progress = require('request-progress');
const tar = require('tar')
// 1677752090771.zip





let allocId = 0;
/**
 * 
 * @param url 
 * @param progress_callback 进程过程中的回调 
 * @param target 文件保存的地址
 * @returns 
 */
function download(){
    const tempPath = path.resolve(__dirname, `./temp/${Date.now()+allocId++}.zip`)
    console.log('tempPath: ', tempPath)
    return new Promise((resolve, reject)=> {
        progress(request("https://bfm-prd-download.oss-cn-hongkong.aliyuncs.com/cot/COT-beta-202302222200.apk"), {})
        .on('progress', createOnProgress())
        .on('error', createErrorCallback(reject))
        .on('end',() => createEndCallback(resolve, reject)(tempPath))
        .pipe(fs.createWriteStream(tempPath, {flags: "wx"})) 
    })
}

/**
 * 创建 pregress 事件监听器
 * @param progress_callback 
 * @returns 
 */
function createOnProgress(){
    return (state) => {
        console.log('state: ', state.percent)
        
    }
}

/**
 * 创建 end 事件监听器
 * @param resolve 
 * @param reject 
 * @returns 
 */
function createEndCallback(resolve , reject){
    return async (tempPath) => {
        console.log('[file-download.cts 开始解压文件]')
        const target = `${path.resolve(__dirname, "./apps")}`
        tar.x({
            file:tempPath,
            gzip: true,
            C: target
        })
        .then(() => {
            console.log('解压完成-')
            resolve(true)
        })
        .catch((err) => console.log('解压失败', err))
    }
}

/**
 * 创建 Error 事件监听器
 * @param reject 
 * @returns 
 */
function createErrorCallback(reject){
    return (err) => {
        console.log('err: ', err)
    }
}

// download()



const extract = require('extract-zip')
 
async function main () {
    tempPath = path.resolve(__dirname, `./temp/1677754021301.zip`)
  try {
    await extract(tempPath, { dir: `${path.resolve(__dirname, "./apps")}` })
    console.log('Extraction complete')
  } catch (err) {
    // handle any errors
  }
}

main()

 