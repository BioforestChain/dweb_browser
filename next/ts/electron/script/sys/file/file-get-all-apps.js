"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getAllApps = void 0;
// 获取全部的 app信息
const promises_1 = __importDefault(require("fs/promises"));
const path_1 = __importDefault(require("path"));
const process_1 = __importDefault(require("process"));
async function getAllApps() {
    return new Promise(async (resolve, reject) => {
        const appsPath = path_1.default.resolve(process_1.default.cwd(), "./apps/infos");
        const foldersName = await promises_1.default.readdir(appsPath);
        const appsInfo = [];
        foldersName.forEach(async (folderName) => {
            const metaData = (await JSON.parse(promises_1.default.readFile(path_1.default.resolve(appsPath, `./${folderName}/package.json`), "utf-8")));
            appsInfo.push(metaData);
        });
        resolve(appsInfo);
    });
}
exports.getAllApps = getAllApps;
