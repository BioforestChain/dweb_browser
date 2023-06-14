
declare global {
  namespace globalThis {
    const __web_browser_api__: {
      getAllWindow(): string;
      createWindow(url: string): number;
      closeWindow(id: number): boolean;
      getFrame(id: number): string;
      setFrame(
        id: number,
        x: number,
        y: number,
        width: number,
        height: number,
        round: number
      ): void;
      setFramesBatch(ops: string): void;

      getVisible(id: number): boolean;
      setVisible(id: number, visible: boolean): void;

      getZIndex(id: number): number;
      setZIndex(id: number, zIndex: number): void;

      getTitle(id: number): string;
      getIcon(id: number): string;
      getUrl(id: number): string;
      setUrl(id: number, url: string): void;

      onTitleChange?: (id: number, title: string) => void;
      onIconChange?: (id: number, src: string) => void;
      onUrlChange?: (id: number, url: string) => void;
    };
  }
}
export { };
if(typeof __web_browser_api__ === "undefined") {
  Object.assign(globalThis, {
    __web_browser_api__: {
      getAllWindow() {
        return "0,1";
      },
      getFrame() {
        return Array.from({ length: 5 }).fill(10).join(",");
      },
      getVisible() {
        return true;
      },
      getZIndex() {
        return 1;
      },
      setFrame() { },
      setFramesBatch() { },
    },
  });
}
