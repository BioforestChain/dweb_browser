export const createJsBlob = (code: string) => {
  const blob = new Blob([code], { type: "text/javascript" });
  const blob_url = URL.createObjectURL(blob);
  return blob_url;
};
