export function isShadowRoot(o: any): o is ShadowRoot {
  return typeof o === 'object' && o !== null && 'host' in o && 'mode' in o; 
}

export function isHTMLElement(o: any): o is HTMLElement{
  return o instanceof HTMLElement;
}

export function isCSSStyleDeclaration(o: any): o is CSSStyleDeclaration{
  return o instanceof CSSStyleDeclaration;
}