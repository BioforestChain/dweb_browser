export type $Insets = {
  left: number;
  top: number;
  right: number;
  bottom: number;
};
export const insetsToDom = (insets: $Insets) => new DOMInsets(insets.left, insets.top, insets.right, insets.bottom);
export const domInsetsToJson = (domInsets: DOMInsets): $Insets => ({
  left: domInsets.left,
  top: domInsets.top,
  right: domInsets.right,
  bottom: domInsets.bottom,
});

export class DOMInsets {
  constructor(readonly left: number, readonly top: number, readonly right: number, readonly bottom: number) {}
  toCss(options?: $InsetsToCssOptions) {
    const { css_var_prefix, density } = formatInsetsToCssOptions(options);
    return [
      [`${css_var_prefix}left`, `${this.left / density}px`],
      [`${css_var_prefix}top`, `${this.top / density}px`],
      [`${css_var_prefix}right`, `${this.right / density}px`],
      [`${css_var_prefix}bottom`, `${this.bottom / density}px`],
    ];
  }
  toCssText(options?: $InsetsToCssOptions) {
    return this.toCss(options)
      .map(([key, value]) => `${key}:${value};`)
      .join(" ");
  }
  setStyle(style: CSSStyleDeclaration, options?: $InsetsToCssOptions) {
    for (const [key, value] of this.toCss(options)) {
      style.setProperty(key, value);
    }
  }
  effect(options: $InsetsToCssOptions) {
    const eleId = options.css_var_prefix?.concat("<<prefix") ?? options.css_var_name?.concat("<<name") ?? "default";
    let ele = document.getElementById(eleId) as HTMLStyleElement | null;
    if (ele == undefined) {
      ele = document.createElement("style");
      document.head.appendChild(ele);
      ele.id = eleId;
    }
    let cssText = `:root {${this.toCssText(options)}}`;
    if (options.layer) {
      cssText = `@layer ${options.layer} {${cssText}}`;
    }
    ele.innerHTML = cssText;
  }
}

interface $InsetsToCssOptions {
  css_var_prefix?: string;
  css_var_name?: string;
  density?: number;
  layer?: string;
}

const formatInsetsToCssOptions = (options: $InsetsToCssOptions = {}) => {
  const css_var_prefix =
    options.css_var_prefix ?? (options.css_var_name ? `--${options.css_var_name}-inset-` : `--inset-`);
  const density = options.density ?? window.devicePixelRatio;
  return { css_var_prefix, density };
};
