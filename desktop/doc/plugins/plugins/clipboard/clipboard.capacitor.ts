import { Clipboard } from '@capacitor/clipboard';
/** 读取剪切板文本 */
const readText = async () => {
  const res = await Clipboard.read();
  if (res.type === 'text/plain') {
    return res.value;
  }
};
/** 写入剪切板文本 */
const writeText = async (text: string) => {
  await Clipboard.write({ string: text });
};

export default { readText, writeText };
