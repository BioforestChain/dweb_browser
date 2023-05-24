"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.download = void 0;
// 下载
const fs_1 = __importDefault(require("fs"));
const promises_1 = __importDefault(require("fs/promises"));
const path_1 = __importDefault(require("path"));
const request_1 = __importDefault(require("request"));
const request_progress_1 = __importDefault(require("request-progress"));
const extract_zip_1 = __importDefault(require("extract-zip"));
/**
 *
 * @param url
 * @param progress_callback 进程过程中的回调
 * @param target 文件保存的地址
 * @returns
 */
function download(url, app_id, progress_callback, appInfo) {
    const tempPath = path_1.default.resolve(__dirname, `../../../temp/${app_id}.zip`);
    return new Promise((resolve, reject) => {
        (0, request_progress_1.default)((0, request_1.default)(url), {})
            .on("progress", createOnProgress(progress_callback))
            .on("error", createErrorCallback(reject))
            .on("end", () => createEndCallback(resolve, reject)(tempPath, app_id, appInfo))
            .pipe(fs_1.default.createWriteStream(tempPath, { flags: "wx" }));
    });
}
exports.download = download;
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
            await (0, extract_zip_1.default)(tempPath, {
                dir: path_1.default.resolve(process.cwd(), `./apps/${app_id}`),
            });
            await promises_1.default.unlink(tempPath);
            await promises_1.default.writeFile(path_1.default.resolve(process.cwd(), `./apps/infos/${_appInfo.id}.json`), appInfo, { encoding: "utf8", flag: "w" });
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
