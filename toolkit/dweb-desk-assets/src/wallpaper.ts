const html = String.raw;
const svg = String.raw;
const isDesktop = (() => {
  try {
    return (navigator as any).userAgentData.mobile == false;
  } catch {
    return false;
  }
})();
const easeOutCubic = (t: number) => {
  const t1 = t - 1;
  return t1 * t1 * t1 + 1;
};

class DwebWallpaperElement extends HTMLElement {
  readonly svgEle;

  constructor() {
    super();
    // Create a shadow root
    const shadow = this.attachShadow({ mode: "open" });
    shadow.innerHTML = html`
      <style>
        :host {
          overflow: hidden;
          --bg-color-1: #fff; /*#93f3ff;*/
          --bg-color-2: #fff; /*#ffca7b;*/
          --bg-mix-blend-mode: hard-light;
          background: linear-gradient(var(--bg-color-deg, 90deg), var(--bg-color-1), var(--bg-color-2));
        }
        :host > svg {
          mix-blend-mode: var(--bg-mix-blend-mode);
          object-fit: cover;
          object-position: center;
          height: 100%;
          width: 100%;
        }

        @media (prefers-color-scheme: dark) {
          :host {
            --bg-color-1: #999; /*#00a9be;*/
            --bg-color-2: #999; /*#dd8400;*/
          }
        }
      </style>
      <svg id="wallpaper-content" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid slice"></svg>
    `;
    const svgEle = shadow.querySelector("svg")!;
    this.svgEle = svgEle;
  }
  private ti: any = -1;
  #anis: Animation[] = [];
  doAni(rectEles: SVGRectElement[]) {
    // this.#anis.forEach((ani) => {
    //   ani.pause();
    // });
    const anis = rectEles.map((ele, index) => this.aniRect(ele, index, rectEles.length));
    this.#anis.forEach((ani) => {
      ani.cancel();
    });
    this.#anis = anis;
    return this.replay();
  }
  replay(
    options: {
      startPlaybackRate?: number;
      endPlaybackRate?: number;
      duration?: number;
      easing?: (p: number) => number;
    } = {},
  ) {
    const { startPlaybackRate = 1, endPlaybackRate = 0, duration = 5000, easing = easeOutCubic } = options;
    const anis = this.#anis;
    return new Promise<void>((resolve) => {
      const startTime = performance.now();
      const down = () => {
        if (ti !== this.ti) {
          resolve();
          return;
        }
        const currentTime = performance.now();
        const progress = Math.min(1, (currentTime - startTime) / duration);
        const playbackRate = startPlaybackRate + easing(progress) * (endPlaybackRate - startPlaybackRate);

        anis.forEach((ani) => ani.updatePlaybackRate(playbackRate));
        if (progress === 1) {
          if (endPlaybackRate === 0) {
            anis.forEach((ani) => ani.pause());
            this.svgEle.pauseAnimations();
          }
          resolve();
        } else {
          this.ti = ti = setTimeout(down);
        }
      };

      // 开始播放所有动画
      this.svgEle.unpauseAnimations();
      anis.forEach((ani) => ani.play());
      // 帧率动画
      let ti = (this.ti = setTimeout(down));
    });
  }

  #sleep(ms: number) {
    return new Promise<void>((resolve) => (this.ti = setTimeout(resolve, ms)));
  }
  config = (this.textContent || "").trim();
  #mixBlendModeMap = (() => {
    const mixBlendModeMap = Array.from({ length: 24 }, () => "hard-light");
    this.config
      .split("\n")
      .map((line) => line.trim())
      // 必须是数字开头
      .filter((line) => /^\d/.test(line))
      .forEach((line) => {
        const [hours, mode] = line.split(":");
        hours
          .split(/[,\s]+/)
          .map((h) => parseInt(h))
          .forEach((hour) => {
            mixBlendModeMap[hour] = mode;
          });
      });

    return mixBlendModeMap.map((mode) => {
      return mode.split(/[\s,]+/).filter((mode) => /^\w/.test(mode))[0] || "hard-light";
    });
  })();

  #colorsMap = (() => {
    const colorsMap = Array.from({ length: 24 }, () => `#043227 #097168 #ffcc88 #fa482e #f4a32e`);
    this.config
      .split("\n")
      .map((line) => line.trim())
      // 必须是数字开头
      .filter((line) => /^\d/.test(line))
      .forEach((line) => {
        const [hours, colors] = line.split(":");
        hours
          .split(/[,\s]+/)
          .map((h) => parseInt(h))
          .forEach((hour) => {
            colorsMap[hour] = colors;
          });
      });

    return colorsMap.map((colors) => {
      return colors
        .trim()
        .split(/[\s,]+/)
        .filter((hex) => hex.startsWith("#"))
        .map((hex) => hex.slice(1))
        .map((hex) => [parseInt(hex.slice(0, 2), 16), parseInt(hex.slice(2, 4), 16), parseInt(hex.slice(4, 6), 16)]);
    });
  })();
  #rectEles: SVGRectElement[] = [];
  #currentConfig: string = "";
  doInit() {
    // const hour = (new Date()).getHours();
    let hour = parseInt(this.getAttribute("hour") || "NaN");
    if (!Number.isFinite(hour)) {
      hour = new Date().getHours();
    }
    const mixBlendMode = this.#mixBlendModeMap[hour % this.#mixBlendModeMap.length];
    const colors = this.#colorsMap[hour % this.#colorsMap.length];

    const config = JSON.stringify({ mixBlendMode, colors });
    if (config === this.#currentConfig) {
      return this.#rectEles;
    }
    this.#currentConfig = config;
    this.style.setProperty("--bg-mix-blend-mode", mixBlendMode);

    this.svgEle.innerHTML = svg`<defs>
      ${colors
        .map(
          (rgb, index) =>
            svg`
          <radialGradient id="Gradient${index}" cx="50%" cy="50%" fx="${rand(0, 10)}%" fy="50%" r=".5">
            <animate attributeName="fx" dur="0s" values="0%;0%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(${rgb}, 1)"></stop>
            <stop offset="100%" stop-color="rgba(${rgb}, 0)"></stop>
          </radialGradient>`,
        )
        .join("\n")}
    </defs>
    ${colors
      .map(
        (_, index) =>
          svg`<rect id="rect1" x="0" y="0" width="100%" height="100%" fill="url(#Gradient${index})"></rect>`,
      )
      .join("\n")}
    `;

    const rectEles: SVGRectElement[] = [];
    this.#rectEles = rectEles;
    this.svgEle.querySelectorAll("rect").forEach((ele, index) => {
      rectEles.push(ele);
      (globalThis as any)["rect" + index] = ele;
    });
    this.svgEle.querySelectorAll("radialGradient").forEach((ele) => {
      this.aniGradient(ele);
    });
    return rectEles;
  }

  private connected = false;
  connectedCallback() {
    const internal = 60 * 60 * 1000;
    this.connected = true;
    (async () => {
      while (this.connected) {
        await this.redraw();
        const now = Date.now();
        const next = internal - (now % internal);
        await this.#sleep(next);
      }
    })();
  }
  async redraw() {
    clearTimeout(this.ti);
    await this.doAni(this.doInit());
  }
  disconnectedCallback() {
    this.connected = false;
    clearTimeout(this.ti);
  }
  aniGradient(gradient: SVGGradientElement) {
    if (isDesktop) {
      const aniEle = gradient.querySelector("animate")!;
      aniEle.setAttribute("dur", rand(20, 30) + "s");
      const start_end = rand(0, 10) + "%";
      const center = rand(0, 10) + "%";
      aniEle.setAttribute("values", `${start_end};${center};${start_end}`);
    }
  }
  aniRect(rect: SVGRectElement, index: number, length: number) {
    const rotateDir = rand() > 0 ? 1 : -1;
    const baseOriX = rand(30, 70);
    const baseOriY = rand(30, 70);
    const scaleBase = (20 * (index + 1)) / length;
    const scaleX = rand(80 + scaleBase, 140);
    const scaleY = rand(80 + scaleBase, 140);
    const style = getComputedStyle(rect);
    console.log(style.transform, style.transformOrigin);
    const ani = rect.animate(
      randomArray(10, (frame) => {
        const keyframe =
          frame.offset === 0 || frame.offset === 1
            ? {
                ...frame,
                transform: style.transform,
                transformOrigin:
                  style.transformOrigin === "0px 0px"
                    ? `${absRand(frame.offset, 0.5, 10) + baseOriX}% ${absRand(frame.offset, 0.5, 10) + baseOriY}%`
                    : style.transformOrigin,
              }
            : {
                ...frame,
                transform: `rotate(${rotateDir * frame.offset * 360}deg) scale(${scaleX}%, ${scaleY}%)`,
                transformOrigin: `${absRand(frame.offset, 0.5, 10) + baseOriX}% ${absRand(frame.offset, 0.5, 10) + baseOriY}%`,
              };

        return keyframe;
      }),
      {
        easing: this.easing,
        fill: "forwards",
        duration: (20 + rand(-5, 5)) * 100,
        iterations: Infinity,
        iterationStart: rand(0, 100),
      },
    );
    return ani;
  }
  readonly easing = "ease-in-out";
}

const randomArray = <R>(len: number, map: (ele: { offset: number }, index: number) => R) =>
  Array.from({ length: len }, (_, index) => map({ offset: index / (len - 1) }, len));
const rand = (min = -1, max = 1) => Math.random() * (max - min) + min;
const absRand = (offset: number, mid: number, size: number) => (mid - Math.abs(mid - offset)) * rand() * size;

customElements.define("dweb-wallpaper", DwebWallpaperElement);
