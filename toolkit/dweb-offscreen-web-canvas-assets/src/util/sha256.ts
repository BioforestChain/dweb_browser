const encoder = new TextEncoder();
const decoder = new TextDecoder();
export const sha256 = async (message: string) => {
  const data = encoder.encode(message);
  const dig = await crypto.subtle.digest("SHA-256", data);
  return decoder.decode(dig);
};
