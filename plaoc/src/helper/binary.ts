/**
 * url replay
 * @param url
 * @returns
 */
export const encodeUri = (url: string) => {
  // TODO 这里的垃圾代码需要修改
  return url
    .replaceAll("#", "%23")
    .replaceAll("{", "%7B")
    .replaceAll("}", "%7D");
};
