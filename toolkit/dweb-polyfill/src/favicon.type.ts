declare global {
  const __native_favicon_kit__:
    | undefined
    | {
        emitChange(href: string): void;
      };
}

export {};
