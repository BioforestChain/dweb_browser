import type {} from "./ios.type.ts";
addEventListener(
  "hashchange",
  function () {
    webkit?.messageHandlers?.navigationChange.postMessage(location.href);
  },
  true
);
addEventListener(
  "popstate",
  function () {
    webkit?.messageHandlers?.navigationChange.postMessage(location.href);
  },
  true
);

const _pushState = History.prototype.pushState;
History.prototype.pushState = function pushState(...args) {
  const result = _pushState.apply(this, args);
  webkit?.messageHandlers?.navigationChange.postMessage(location.href);
  return result;
};
