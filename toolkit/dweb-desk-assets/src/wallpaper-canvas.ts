const easeOutCubic = (t: number) => {
  const t1 = t - 1;
  return t1 * t1 * t1 + 1;
};
const easeInOutCubic = (t: number) => {
  return t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
};
const linear = (t: number) => t;
const calcNumberAnimation = (start: number, end: number, progress: number) => {
  if (progress <= 0) {
    return start;
  }
  if (progress >= 1) {
    return end;
  }
  if (start === end) {
    return end;
  }
  return (end - start) * progress + start;
};
const calcMatrixAngle = (matrix: DOMMatrix) => Math.atan2(matrix.b, matrix.a);
const calcMatrixScaleX = (matrix: DOMMatrix) => Math.sqrt(matrix.a * matrix.a + matrix.c * matrix.c);
const calcMatrixScaleY = (matrix: DOMMatrix) => Math.sqrt(matrix.b * matrix.b + matrix.d * matrix.d);

const keyframesToTransform = <T extends { offset: number }>(
  keyframes: T[],
  effect: (origin: CanvasRectAttributes, start: T, end: T, progress: number) => CanvasRectAttributes,
) => {
  const getKeyframePair = (keyframes: T[], fraction: number) => {
    const sortedKeyframes = keyframes.slice().sort((a, b) => b.offset - a.offset);
    const startIndex = sortedKeyframes.findIndex((v) => fraction >= v.offset);
    const start = sortedKeyframes[startIndex];
    const end = sortedKeyframes[startIndex - 1] || start;
    return { start, end };
  };

  return (origin: CanvasRectAttributes, fraction: number) => {
    const { start, end } = getKeyframePair(keyframes, fraction);
    return effect(origin, start, end, end === start ? 1 : (fraction - start.offset) / (end.offset - start.offset));
  };
};

class CanvasAnimation {
  constructor(
    readonly name: string,
    readonly target: CanvasRectElement,
    readonly transform: (origin: CanvasRectAttributes, fraction: number) => CanvasRectAttributes,
    readonly duration: number = 1000,
    readonly easing: (t: number) => number = (t) => t,
    readonly iterations: number = 1,
    readonly iterationStart: number = 0,
  ) {}
  /**
   * 播放速率
   */
  playbackRate = 1;
  /**
   * 播放的动画时长
   */
  playDuration: number = 0;
  /**
   * 是否暂停动画
   */
  isPaused = false;
  /**
   * 上一帧的动画时间
   */
  preAnimationTime = performance.now();
  unpause() {
    if (false === this.isPaused) {
      return;
    }
    this.isPaused = false;
    this.preAnimationTime = performance.now();
  }
  pause() {
    if (this.isPaused) {
      return;
    }
    this.isPaused = true;
  }
  cancel() {
    this.target.animations = this.target.animations.filter((ani) => ani !== this);
  }
  calcEffect(attrs: CanvasRectAttributes, aniDuration = this.playDuration + this.iterationStart) {
    return this.transform(attrs, this.calcFraction(aniDuration));
  }
  calcFraction(aniDuration = this.playDuration + this.iterationStart) {
    const progress = (aniDuration / this.duration) % 1;
    const fraction = this.easing(progress);
    return fraction;
  }
  calc(attrs: CanvasRectAttributes, now: number = performance.now()) {
    const frameDuration = (now - this.preAnimationTime) * this.playbackRate;
    this.preAnimationTime = now;

    const { duration, iterations, iterationStart } = this;
    const maxPlayDuration = iterations * duration - iterationStart;

    if (this.playDuration >= maxPlayDuration) {
      return attrs;
    }
    this.playDuration = Math.min(this.playDuration + frameDuration, maxPlayDuration);
    return this.calcEffect(attrs);
  }
}

class CanvasRectAttributes {
  constructor(
    readonly width: number,
    readonly height: number,
    readonly fillStyle: Readonly<{
      /** 终点圆(大圆) x */
      cx: number;
      /** 终点圆(大圆) y */
      cy: number;
      /** 起点圆(小圆) x */
      fr: number;
      /** 起点圆(小圆) y */
      fx: number;
      /** 起点圆(小圆) 半径 */
      fy: number;
      /** 终点圆(大圆) 半径 */
      r: number;
      stopColors: ReadonlyArray<Readonly<{ offset: number; color: string }>>;
    }>,
    readonly transform = new DOMMatrix("none"),
    readonly transformOriginX = 0.5,
    readonly transformOriginY = 0.5,
  ) {}
  copy(attrs: {
    width?: CanvasRectAttributes["width"];
    height?: CanvasRectAttributes["height"];
    fillStyle?: CanvasRectAttributes["fillStyle"];
    transform?: CanvasRectAttributes["transform"];
    transformOriginX?: CanvasRectAttributes["transformOriginX"];
    transformOriginY?: CanvasRectAttributes["transformOriginY"];
  }) {
    return new CanvasRectAttributes(
      attrs.width ?? this.width,
      attrs.height ?? this.height,
      attrs.fillStyle ?? this.fillStyle,
      attrs.transform ?? this.transform,
      attrs.transformOriginX ?? this.transformOriginX,
      attrs.transformOriginY ?? this.transformOriginY,
    );
  }
}
class CanvasRectElement {
  constructor(readonly attrs: CanvasRectAttributes) {}
  animations: CanvasAnimation[] = [];
  // scaleX = 1
  // scaleY = 1
  // rotate = 0
  replaceAnimation(animation: CanvasAnimation) {
    this.animations = this.animations.filter((ani) => ani.name !== animation.name);
    this.animations.push(animation);
  }
  getAnimated() {
    const now = performance.now();
    let attrs = this.attrs;
    for (const ani of this.animations) {
      attrs = ani.calc(attrs, now);
    }
    return attrs;
  }

  static draw(canvas: HTMLCanvasElement, ctx: CanvasRenderingContext2D, ele: CanvasRectElement) {
    const attrs = ele.getAnimated();
    const { cx, cy, fr, fx, fy, r, stopColors } = attrs.fillStyle;
    const { width: W, height: H } = canvas;
    const SIZE_W = Math.max(W, H);
    const SIZE_H = SIZE_W;
    const width = SIZE_W * attrs.width;
    const height = SIZE_H * attrs.height;
    const size = Math.max(width, height);
    const min_size = Math.min(width, height);
    const gradient = ctx.createRadialGradient(
      fx * min_size,
      fy * min_size,
      fr * min_size,
      cx * min_size,
      cy * min_size,
      r * min_size,
    );
    for (const sc of stopColors) {
      gradient.addColorStop(sc.offset, sc.color);
    }
    ctx.fillStyle = gradient;
    const matrix = new DOMMatrix();
    const transformOriginX = SIZE_W * attrs.transformOriginX;
    const transformOriginY = SIZE_H * attrs.transformOriginY;
    matrix.translateSelf(transformOriginX, transformOriginY);
    matrix.multiplySelf(attrs.transform);
    matrix.translateSelf(-transformOriginX, -transformOriginY);
    matrix.translateSelf((W - SIZE_W) / 2, (H - SIZE_H) / 2);
    ctx.transform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f);
    ctx.fillRect(0, 0, width, height);
    ctx.resetTransform();
    return gradient;
  }
}

const looper = new (class Looper {
  private tis = new Map<number, { type: "timeout" | "frame"; value: any }>();
  private ti_acc = 0;
  setTimeout(callback: () => void, ms?: number) {
    const ti = this.ti_acc++;
    const timeout = setTimeout(() => {
      this.tis.delete(ti);
      callback();
    }, ms);
    this.tis.set(ti, { type: "timeout", value: timeout });
    return ti;
  }
  clearTimeout(ti: number) {
    const timeout = this.tis.get(ti);
    if (timeout?.type !== "timeout") {
      this.tis.delete(ti);
      clearTimeout(timeout!.value);
      return true;
    }
    return false;
  }
  requestAnimationFrame(callback: (time: number) => void) {
    const ti = this.ti_acc++;
    const req = requestAnimationFrame((time) => {
      this.tis.delete(ti);
      callback(time);
    });
    this.tis.set(ti, { type: "frame", value: req });
    return ti;
  }
  cancelAnimationFrame(ti: number) {
    const req = this.tis.get(ti);
    if (req?.type !== "frame") {
      this.tis.delete(ti);
      cancelAnimationFrame(req!.value);
      return true;
    }
    return false;
  }
  sleep(ms: number) {
    let ti: number;
    let reject: (reason?: any) => void;
    const promise = new Promise<void>((resolve, _reject) => {
      ti = this.setTimeout(resolve, ms);
      reject = _reject;
    });
    return Object.assign(promise, {
      abort: (reason?: any) => {
        if (this.clearTimeout(ti)) {
          reject(reason);
        }
      },
    });
  }
  cancelAll() {
    for (const [ti, ref] of this.tis) {
      switch (ref.type) {
        case "frame":
          cancelAnimationFrame(ti);
          break;
        case "timeout":
          this.clearTimeout(ti);
          break;
      }
    }
    this.tis.clear();
  }
})();

export class DwebWallpaperElement extends HTMLElement {
  readonly looper = looper;
  readonly canvasEle;
  readonly renderCtx;

  constructor() {
    super();
    // Create a shadow root
    const shadow = this.attachShadow({ mode: "open" });
    const html = String.raw;

    const propertys = ["--bg-img-deg"];
    try {
      CSS.registerProperty({
        name: "--bg-img-deg",
        syntax: "<angle>",
        inherits: false,
        initialValue: "0deg",
      });
      for (let i = 0; i < 10; i++) {
        propertys.push(`--bg-img-color-${i}`);
        CSS.registerProperty({
          name: `--bg-img-color-${i}`,
          syntax: "<color>",
          inherits: false,
          initialValue: "rgba(0, 0, 0, 0)",
        });
      }
    } catch {
      //
    }
    shadow.innerHTML = html`
      <style>
        :host {
          display: block;
          overflow: hidden;
          --bg-mix-blend-mode: hard-light;
          --bg-img-deg: 0deg;
          background: var(--bg-img);
          background-color: #fff;
          transition-duration: 2s;
          transition-timing-function: ease-in-out;
          transition-property: ${propertys.join(", ")};
        }
        :host > canvas {
          /* mix-blend-mode: var(--bg-mix-blend-mode); */
          object-fit: cover;
          object-position: center;
          height: 100%;
          width: 100%;
        }

        @media (prefers-color-scheme: dark) {
          :host {
            background-color: #999;
          }
        }
      </style>
      <canvas id="wallpaper-content"></canvas>
    `;
    const canvasEle = shadow.querySelector("canvas")!;
    this.canvasEle = canvasEle;
    this.renderCtx = canvasEle.getContext("2d")!;
  }
  #anis: CanvasAnimation[] = [];
  doAni(rectEles: CanvasRectElement[]) {
    const anis = rectEles.map((ele, index) => this.aniRect(ele, index, rectEles.length));
    this.#anis.forEach((ani) => ani.cancel());
    this.#anis = anis;
    return this.replay();
  }
  #replay_ti = -1;
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
      const down = (currentTime: number) => {
        if (ti !== this.#replay_ti) {
          resolve();
          return;
        }
        const progress = Math.min(1, (currentTime - startTime) / duration);
        const playbackRate = startPlaybackRate + easing(progress) * (endPlaybackRate - startPlaybackRate);

        anis.forEach((ani) => (ani.playbackRate = playbackRate));
        if (progress === 1) {
          if (endPlaybackRate === 0) {
            anis.forEach((ani) => ani.pause());
          }
          resolve();
        } else {
          this.#replay_ti = ti = looper.requestAnimationFrame(down);
        }

        /// 执行渲染
        this.#draw();
      };

      // 调整帧率动画并渲染
      anis.forEach((ani) => ani.unpause());
      let ti = (this.#replay_ti = looper.requestAnimationFrame(down));
    });
  }

  #draw() {
    this.renderCtx.clearRect(0, 0, this.canvasEle.width, this.canvasEle.height);
    this.renderCtx.globalCompositeOperation = this.#mixBlendMode;
    for (const rect of this.#rectEles) {
      CanvasRectElement.draw(this.canvasEle, this.renderCtx, rect);
    }
  }

  get config() {
    return (this.textContent || "").trim();
  }
  #calcMixBlendModeMap() {
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
      return (mode.split(/[\s,]+/).filter((mode) => /^\w/.test(mode))[0] || "hard-light") as GlobalCompositeOperation;
    });
  }

  #calcColorsMap() {
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
  }
  #rectEles: CanvasRectElement[] = [];
  #mixBlendMode: GlobalCompositeOperation = "source-over";
  #background = "linear-gradient(var(--bg-img-deg, 0deg), #fff, #fff)";
  doInit() {
    // const hour = (new Date()).getHours();
    let hour = parseInt(this.getAttribute("hour") || "NaN");
    if (!Number.isFinite(hour)) {
      hour = new Date().getHours();
    }
    let minutes = parseInt(this.getAttribute("minutes") || "NaN");
    if (!Number.isFinite(minutes)) {
      minutes = new Date().getMinutes();
    }
    this.style.setProperty("--bg-img-deg", `${(minutes / 60) * 360}deg`);

    const mixBlendModeMap = this.#calcMixBlendModeMap();
    const mixBlendMode = mixBlendModeMap[hour % mixBlendModeMap.length];
    const colorsMap = this.#calcColorsMap();
    const colors = colorsMap[hour % colorsMap.length];
    const background = `linear-gradient(var(--bg-img-deg, 0deg), ${colors.map((rgb, i) => `var(--bg-img-color-${i}, rgba(${rgb},1))`).join(", ")})`;
    colors.forEach((rgb, i) => {
      this.style.setProperty(`--bg-img-color-${i}`, `rgba(${rgb},0.2)`);
    });

    if (mixBlendMode === this.#mixBlendMode && background === this.#background) {
      return this.#rectEles;
    }
    this.#mixBlendMode = mixBlendMode;
    this.#background = background;
    this.style.setProperty("--bg-mix-blend-mode", mixBlendMode);
    this.style.setProperty("background", background);
    this.#rectEles = colors.map((rgb) => {
      return new CanvasRectElement(
        new CanvasRectAttributes(1, 1, {
          cx: 0.5,
          cy: 0.5,
          r: 0.5,
          fx: rand(0, 10) / 100,
          fy: 0.5,
          fr: 0,
          stopColors: [
            { color: `rgba(${rgb},1)`, offset: 0 },
            { color: `rgba(${rgb.map((c) => Math.min(Math.max(0, c + rand(-0, 20))))},0.0)`, offset: 1 },
          ],
        }),
      );
    });
    this.#rectEles.forEach((ele) => {
      this.aniGradient(ele);
    });
    return this.#rectEles;
  }

  private resizeOb?: ResizeObserver;
  private connected = false;
  connectedCallback() {
    const resizeOb = new ResizeObserver((es) => {
      const { width, height } = es[0].contentRect;
      this.canvasEle.width = width; //* devicePixelRatio;
      this.canvasEle.height = height; //* devicePixelRatio;
      this.#draw();
    });
    resizeOb.observe(this);
    this.resizeOb = resizeOb;

    const internal = 5 * 60 * 1000; // 五分钟绘制一次
    this.connected = true;
    (async () => {
      while (this.connected) {
        await this.redraw();
        const now = Date.now();
        const next = internal - (now % internal);
        await looper.sleep(next);
      }
    })();
  }
  async redraw() {
    looper.cancelAll();
    await this.doAni(this.doInit());
  }
  disconnectedCallback() {
    const resizeOb = this.resizeOb;
    if (resizeOb) {
      resizeOb.unobserve(this);
      resizeOb.disconnect();
      this.resizeOb = undefined;
    }
    this.connected = false;
    looper.cancelAll();
  }
  aniGradient(rect: CanvasRectElement) {
    const start = rand(0, 5) / 100;
    rect.replaceAnimation(
      new CanvasAnimation(
        "gradient",
        rect,
        keyframesToTransform(
          randomArray(10, (frame) => {
            if (frame.offset === 0 || frame.offset === 1) {
              return { ...frame, fx: start };
            }
            return { ...frame, fx: rand(0, 10) / 100 };
          }),
          (origin, start, end, progress) => {
            return origin.copy({
              fillStyle: {
                ...origin.fillStyle,
                fx: calcNumberAnimation(start.fx, end.fx, progress),
              },
            });
          },
        ),
        rand(20, 30) * 1000,
        linear,
        Infinity,
        rand(0, 100000),
      ),
    );
  }
  aniRect(rect: CanvasRectElement, index: number, length: number) {
    const rotateDir = rand() > 0 ? 1 : -1;
    const scaleBase = (20 * (index + 1)) / length;
    const scaleX = () => rand(80 + scaleBase, 140) / 100;
    const scaleY = () => rand(80 + scaleBase, 140) / 100;
    const startRect = rect.animations.find((ani) => ani.name == "ani-rect")?.calcEffect(rect.attrs);
    const startScaleX = startRect ? calcMatrixScaleX(startRect.transform) : scaleX();
    const startScaleY = startRect ? calcMatrixScaleY(startRect.transform) : scaleY();
    const startRotate = startRect ? calcMatrixAngle(startRect.transform) : rand(0, 360);
    const startRectTransformOriginX = startRect ? startRect.transformOriginX : rand(0.4, 0.6);
    const startRectTransformOriginY = startRect ? startRect.transformOriginY : rand(0.4, 0.6);

    const keyframes = randomArray(rand(3, 10), (frame) => {
      if (frame.offset === 0) {
        return {
          ...frame,
          rotate: startRotate,
          scaleX: startScaleX,
          scaleY: startScaleY,
          transformOriginX: startRectTransformOriginX,
          transformOriginY: startRectTransformOriginY,
        };
      }
      if (frame.offset === 1) {
        return {
          ...frame,
          rotate: startRotate + rotateDir * frame.offset * 360,
          scaleX: startScaleX,
          scaleY: startScaleY,
          transformOriginX: startRectTransformOriginX,
          transformOriginY: startRectTransformOriginY,
        };
      }
      return {
        ...frame,
        rotate: startRotate + rotateDir * frame.offset * 360,
        scaleX: scaleX(),
        scaleY: scaleY(),
        transformOriginX: (absMax(frame.offset, 0.5) / 10) * rand() + startRectTransformOriginX,
        transformOriginY: (absMax(frame.offset, 0.5) / 10) * rand() + startRectTransformOriginY,
      };
    });

    const duration = (10000 + keyframes.length * 2000) * rand(0.8, 1.2);
    const ani = new CanvasAnimation(
      "ani-rect",
      rect,
      keyframesToTransform(keyframes, (origin, start, end, progress) => {
        if (progress === 0) {
          const startTransform = new DOMMatrix();
          startTransform.rotateAxisAngleSelf(0, 0, 0, start.rotate);
          startTransform.scaleSelf(start.scaleX, start.scaleY);
          return origin.copy({
            transformOriginX: start.transformOriginX,
            transformOriginY: start.transformOriginY,
            transform: startTransform,
          });
        }
        if (progress === 1) {
          const endTransform = new DOMMatrix();
          endTransform.rotateAxisAngleSelf(0, 0, 0, end.rotate);
          endTransform.scaleSelf(end.scaleX, end.scaleY);
          return origin.copy({
            transformOriginX: end.transformOriginX,
            transformOriginY: end.transformOriginY,
            transform: endTransform,
          });
        }
        const rotate = calcNumberAnimation(start.rotate, end.rotate, progress);
        const scaleX = calcNumberAnimation(start.scaleX, end.scaleX, progress);
        const scaleY = calcNumberAnimation(start.scaleY, end.scaleY, progress);

        const transform = new DOMMatrix();
        transform.rotateAxisAngleSelf(0, 0, 1, rotate);
        transform.scaleSelf(scaleX, scaleY);

        return origin.copy({
          transform: transform,
          transformOriginX: calcNumberAnimation(start.transformOriginX, end.transformOriginX, progress),
          transformOriginY: calcNumberAnimation(start.transformOriginY, end.transformOriginY, progress),
        });
      }),
      duration,
      easeInOutCubic,
      Infinity,
      rand(0, duration),
    );
    rect.replaceAnimation(ani);
    return ani;
  }
}

const randomArray = <R>(len: number, map: (ele: { offset: number }, index: number) => R) =>
  Array.from({ length: (len = Math.round(len)) }, (_, index) => map({ offset: index / (len - 1) }, len));
const rand = (min = -1, max = 1) => Math.random() * (max - min) + min;
const absMax = (val: number, max: number) => max - Math.abs(max - val);

customElements.define("dweb-wallpaper", DwebWallpaperElement);
