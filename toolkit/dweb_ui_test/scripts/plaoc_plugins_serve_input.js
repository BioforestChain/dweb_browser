// 未找到获取本地ip地址的方式，暂时通过外部传递
let dwebLink = "dweb://install?url=";
dwebLink += `http://${addr}:8096/metadata.json`;
output.dwebLink = dwebLink;
