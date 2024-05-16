const html = String.raw;
class DwebWallpaperElement extends HTMLElement {
  readonly svg;
  readonly rects;

  constructor() {
    super();
    // Create a shadow root
    const shadow = this.attachShadow({ mode: "open" });
    shadow.innerHTML = html`
      <style>
        :host {
          position: absolute;
          width: 100%;
          height: 100%;
          z-index: 0;
          pointer-events: none;
          top: 0;
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
            --bg-color-1: #333; /*#00a9be;*/
            --bg-color-2: #333; /*#dd8400;*/
          }
        }
      </style>
      <svg id="wallpaper-content" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid slice">
        <defs>
          <radialGradient id="Gradient1" cx="50%" cy="50%" fx="0.441602%" fy="50%" r=".5">
            <animate attributeName="fx" dur="34s" values="0%;3%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(255, 0, 255, 1)"></stop>
            <stop offset="100%" stop-color="rgba(255, 0, 255, 0)"></stop>
          </radialGradient>
          <radialGradient id="Gradient2" cx="50%" cy="50%" fx="2.68147%" fy="50%" r=".5">
            <animate attributeName="fx" dur="23.5s" values="0%;3%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(255, 255, 0, 1)"></stop>
            <stop offset="100%" stop-color="rgba(255, 255, 0, 0)"></stop>
          </radialGradient>
          <radialGradient id="Gradient3" cx="50%" cy="50%" fx="0.836536%" fy="50%" r=".5">
            <animate attributeName="fx" dur="21.5s" values="0%;3%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(0, 255, 255, 1)"></stop>
            <stop offset="100%" stop-color="rgba(0, 255, 255, 0)"></stop>
          </radialGradient>
          <radialGradient id="Gradient4" cx="50%" cy="50%" fx="4.56417%" fy="50%" r=".5">
            <animate attributeName="fx" dur="23s" values="0%;5%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(0, 255, 0, 1)"></stop>
            <stop offset="100%" stop-color="rgba(0, 255, 0, 0)"></stop>
          </radialGradient>
          <radialGradient id="Gradient5" cx="50%" cy="50%" fx="2.65405%" fy="50%" r=".5">
            <animate attributeName="fx" dur="24.5s" values="0%;5%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(0,0,255, 1)"></stop>
            <stop offset="100%" stop-color="rgba(0,0,255, 0)"></stop>
          </radialGradient>
          <radialGradient id="Gradient6" cx="50%" cy="50%" fx="0.981338%" fy="50%" r=".5">
            <animate attributeName="fx" dur="25.5s" values="0%;5%;0%" repeatCount="indefinite"></animate>
            <stop offset="0%" stop-color="rgba(255,0,0, 1)"></stop>
            <stop offset="100%" stop-color="rgba(255,0,0, 0)"></stop>
          </radialGradient>
        </defs>
        <rect id="rect1" x="0" y="0" width="100%" height="100%" fill="url(#Gradient1)"></rect>
        <rect id="rect2" x="0" y="0" width="100%" height="100%" fill="url(#Gradient2)"></rect>
        <rect id="rect3" x="0" y="0" width="100%" height="100%" fill="url(#Gradient3)"></rect>
        <rect id="rect4" x="0" y="0" width="100%" height="100%" fill="url(#Gradient4)"></rect>
        <rect id="rect5" x="0" y="0" width="100%" height="100%" fill="url(#Gradient5)"></rect>
        <rect id="rect6" x="0" y="0" width="100%" height="100%" fill="url(#Gradient6)"></rect>
      </svg>
    `;
    const svg = shadow.querySelector("svg")!;
    this.svg = svg;
    const rects: SVGRectElement[] = [];
    this.rects = rects;
    svg.querySelectorAll("rect").forEach((ele, index) => {
      rects.push(ele);
      (globalThis as any)["rect" + index] = ele;
    });
    svg.querySelectorAll("radialGradient").forEach((ele) => {
      this.aniGradient(ele);
    });
  }
  readonly anis: Animation[] = [];
  private ti?: any;
  doAni() {
    return new Promise<void>((resolve) => {
      if (this.anis.length === 0) {
        this.anis.push(...this.rects.map((ele) => this.aniRect(ele)));
      }
      const anis = this.anis;

      let playbackRate = 1;
      this.svg.unpauseAnimations();
      anis.forEach((ani) => ani.play());
      const down = () => {
        playbackRate *= 0.99;
        if (playbackRate > 0.0001) {
          anis.forEach((ani) => ani.updatePlaybackRate(playbackRate));
          this.ti = setTimeout(down);
        } else {
          anis.forEach((ani) => ani.pause());
          this.svg.pauseAnimations();
          resolve();
        }
      };
      down();
    });
  }
  #sleep(ms: number) {
    return new Promise<void>((resolve) => (this.ti = setTimeout(resolve, ms)));
  }

  private connected = false;
  connectedCallback() {
    const internal = 60 * 60 * 1000;
    this.connected = true;
    (async () => {
      while (this.connected) {
        await this.doAni();
        const now = Date.now();
        const next = internal - (now % internal);
        await this.#sleep(next);
      }
    })();
  }
  disconnectedCallback() {
    this.connected = false;
    clearTimeout(this.ti);
  }
  aniGradient(gradient: SVGGradientElement) {
    const aniEle = gradient.querySelector("animate")!;
    aniEle.setAttribute("dur", rand(20, 30) + "s");
    const start_end = rand(0, 10) + "%";
    const center = rand(0, 10) + "%";
    aniEle.setAttribute("values", `${start_end};${center};${start_end}`);
  }
  aniRect(rect: SVGRectElement) {
    const rotateDir = rand() > 0 ? 1 : -1;
    const baseOriX = rand(30, 70);
    const baseOriY = rand(30, 70);
    const ani = rect.animate(
      randomArray(10, (frame) => {
        const keyframe = {
          ...frame,
          transform: `rotate(${rotateDir * frame.offset * 360}deg)`,
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
