(() => {
  let latestActiveElement: HTMLOrSVGElement | null = null;
  self.addEventListener("blur", () => {
    const blurAbleEle = document.activeElement as unknown as HTMLOrSVGElement | null;
    if (typeof blurAbleEle?.blur === "function") {
      latestActiveElement = blurAbleEle;
      requestAnimationFrame(() => {
        blurAbleEle.blur();
      });
    }
  });
  self.addEventListener("focus", () => {
    if (latestActiveElement !== null) {
      const focusAbleEle = latestActiveElement;
      latestActiveElement = null;
      requestAnimationFrame(() => {
        focusAbleEle.focus();
      });
    }
  });
})();
