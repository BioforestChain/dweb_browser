export const getPlaocServerDir = (isLive: boolean) => {
  const moduleSpecifier = isLive ? "@plaoc/server/plaoc.server.dev.js" : "@plaoc/server/plaoc.server.js";
  return new URL("./", import.meta.resolve(moduleSpecifier)).pathname;
};
