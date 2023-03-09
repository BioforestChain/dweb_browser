/**
 * url replay
 * @param url 
 * @returns 
 */
export const encodeUri = (url: string) => {
  return url.replaceAll("#", "%23")
}
