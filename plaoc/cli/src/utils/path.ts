
/**
 * ## slash
 * Convert Windows backslash paths to slash paths: `foo\\bar` ➔ `foo/bar`
 * @fork https://github.com/sindresorhus/slash/blob/main/index.js
 */
export function slash(path: string) {
  const isExtendedLengthPath = /^\\\\\?\\/.test(path);
  // deno-lint-ignore no-control-regex
  const hasNonAscii = /[^\u0000-\u0080]+/.test(path); // eslint-disable-line no-control-regex

  if (isExtendedLengthPath || hasNonAscii) {
    return path;
  }

  return path.replace(/\\+/g, "/");
}

/**
 * 追加 /
 * @param pth
 * @returns
 */
export function appendForwardSlash(pth: string) {
  return pth.endsWith("/") ? pth : pth + "/";
}
