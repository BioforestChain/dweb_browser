import os from "node:os";

export const getLocalIp = async () => {
  let addr = "";
  for await (const info of Object.values(os.networkInterfaces())
    .flat() // 返回一个新数组，其中所有子数组元素都以递归方式连接到其中，直到指定的深度。
    .filter((info) => info?.family === "IPv4")) {
    if (info?.address !== "127.0.0.1") {
      addr = info?.address ?? "";
      break;
    }
  }
  return addr;
};
