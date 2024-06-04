export const $makeFetchExtends = <M extends unknown = unknown>(exts: $FetchExtends<M>) => {
  return exts;
};

export const $makeExtends = <T>() => {
  return <M extends unknown = unknown>(exts: M & ThisType<T & M>) => {
    return exts;
  };
};

export type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

export const fetchBaseExtends = $makeFetchExtends({
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
      // deno-lint-ignore no-debugger
      debugger;
      throw err;
    }
  },
});
