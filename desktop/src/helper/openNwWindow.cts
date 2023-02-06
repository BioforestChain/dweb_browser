
export const openNwWindow = (
  url: `${string}.html`,
  options?: nw.IWindowOptions
) => {
  return new Promise<nw.Window>((resolve) => {
    nw.Window.open(url, options, resolve);
  });
};
