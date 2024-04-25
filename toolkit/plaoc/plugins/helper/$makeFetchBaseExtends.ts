import { $makeExtends } from "./$makeExtends.ts";

export const fetchBaseExtends = $makeExtends<Promise<Response>>()({
  async number() {
    const text = await this.text();
    return +text;
  },
  async ok() {
    const response = await this;
    if (response.status >= 400) {
      throw response.statusText || (await response.text());
    } else {
      return response;
    }
  },
  async void() {
    await this.ok();
  },
  async text() {
    const ok = await this.ok();
    return ok.text();
  },
  async binary() {
    const ok = await this.ok();
    return ok.arrayBuffer();
  },
  async boolean() {
    const text = await this.text();
    return text === "true"; // JSON.stringify(true)
  },
  async object<T>() {
    const ok = await this.ok();
    try {
      return (await ok.json()) as T;
    } catch (err) {
      console.error("fail to object", ok.url);
      throw err;
    }
  },
});
