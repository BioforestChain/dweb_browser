export function isShadowRoot(o: ShadowRoot  | unknown): o is ShadowRoot {
  return typeof o === "object" && o !== null && "host" in o && "mode" in o;
}

export function isHTMLElement(o: HTMLElement | unknown): o is HTMLElement {
  return o instanceof HTMLElement;
}

export function isCSSStyleDeclaration(o: CSSStyleDeclaration | unknown): o is CSSStyleDeclaration {
  return o instanceof CSSStyleDeclaration;
}
