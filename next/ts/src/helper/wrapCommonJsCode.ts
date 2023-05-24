
export const wrapCommonJsCode = (
  common_js_code: string,
  options: {
    before?: string;
    after?: string;
  } = {}
) => {
  const { before = "", after = "" } = options;

  return `${before};((module,exports=module.exports)=>{${common_js_code.replaceAll(
    `"use strict";`,
    ""
  )};return module.exports})({exports:{}})${after};`;
};
