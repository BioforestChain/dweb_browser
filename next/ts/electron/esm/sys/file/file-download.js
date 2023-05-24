// 下载
import fs from "fs";
import fsPromises from "fs/promises";
import path from "path";
import request from "request";
import progress from "request-progress";
import extract from "extract-zip";
/**
 *
 * @param url
 * @param progress_callback 进程过程中的回调
 * @param target 文件保存的地址
 * @returns
 */
export function download(url, app_id, progress_callback, appInfo) {
    const tempPath = path.resolve(__dirname, `../../../temp/${app_id}.zip`);
    return new Promise((resolve, reject) => {
        progress(request(url), {})
            .on("progress", createOnProgress(progress_callback))
            .on("error", createErrorCallback(reject))
            .on("end", () => createEndCallback(resolve, reject)(tempPath, app_id, appInfo))
            .pipe(fs.createWriteStream(tempPath, { flags: "wx" }));
    });
}
/**
 * 创建 pregress 事件监听器
 * @param progress_callback
 * @returns
 */
function createOnProgress(progress_callback) {
    return (state) => {
        progress_callback(state);
    };
}
/**
 * 创建 end 事件监听器
 * @param resolve
 * @param reject
 * @returns
 */
function createEndCallback(resolve, reject) {
    return async (tempPath, app_id, appInfo) => {
        const _appInfo = JSON.parse(appInfo);
        try {
            await extract(tempPath, {
                dir: path.resolve(process.cwd(), `./apps/${app_id}`),
            });
            await fsPromises.unlink(tempPath);
            await fsPromises.writeFile(path.resolve(process.cwd(), `./apps/infos/${_appInfo.id}.json`), appInfo, { encoding: "utf8", flag: "w" });
            resolve(true);
        }
        catch (err) {
            reject(err);
        }
    };
}
/**
 * 创建 Error 事件监听器
 * @param reject
 * @returns
 */
function createErrorCallback(reject) {
    return (err) => {
        reject(err);
    };
}
// manifest： Manifest {
//     xml: XmlElement {
//       attributes: {
//         versionCode: 1,
//         versionName: '1.1.400',
//         compileSdkVersion: 32,
//         compileSdkVersionCodename: '12',
//         package: 'info.bfmeta.cot',
//         platformBuildVersionCode: 32,
//         platformBuildVersionName: 12
//       },
//       children: {
//         'uses-sdk': [Array],
//         'uses-permission': [Array],
//         queries: [Array],
//         'uses-feature': [Array],
//         application: [Array]
//       },
//       tag: 'manifest'
//     }
//   }
// console.log(`package = ${manifest.package}`);
// console.log(`versionCode = ${manifest.versionCode}`);
// console.log(`versionName = ${manifest.versionName}`);
// console.log('manifest：', manifest)
// for properties which haven't any existing accessors you can use the raw binary xml
// console.log(JSON.stringify(manifest.raw, null, 4));
