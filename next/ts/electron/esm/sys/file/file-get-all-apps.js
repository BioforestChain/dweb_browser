// 获取全部的 app信息
import fsPromises from "fs/promises";
import path from "path";
import process from "process";
export async function getAllApps() {
    return new Promise(async (resolve, reject) => {
        const appsPath = path.resolve(process.cwd(), "./apps/infos");
        const foldersName = await fsPromises.readdir(appsPath);
        const appsInfo = [];
        foldersName.forEach(async (folderName) => {
            const metaData = (await JSON.parse(fsPromises.readFile(path.resolve(appsPath, `./${folderName}/package.json`), "utf-8")));
            appsInfo.push(metaData);
        });
        resolve(appsInfo);
    });
}
