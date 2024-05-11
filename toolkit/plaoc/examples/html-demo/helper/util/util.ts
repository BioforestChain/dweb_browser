export const b64toBlob = async (b64Data: string) => {
  const base64Response = await fetch(b64Data);
  return await base64Response.blob();
};
