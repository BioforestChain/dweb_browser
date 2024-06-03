export const getDayHoursConfig = () => {
  const resultColors: string[] = [];
  const hours = Array.from({ length: 24 }, (_, i) => i);
  for (const hour of hours) {
    const nearColors = findNearColors(hour);
    let colors = nearColors[0].colors;
    if (21 === hour) {
      console.log(hour, nearColors);
    }
    if (nearColors.length > 1) {
      colors = mixColorsByRgb(nearColors[0].colors, nearColors[1].colors, nearColors[0].rate, nearColors[1].rate);
    }
    resultColors.push(`${hour}: overlay ${colors.join(" ")}`);
  }
  return resultColors.join("\n");
};

export const findNearColors = (hour: number) => {
  const referenceColors = REFERENCE_COLORS.map((r) => {
    return {
      ...r,
      diffHour: diffHour(r.hour, hour),
    };
  });
  const realtivedColors = referenceColors.slice().sort((a, b) => a.diffHour - b.diffHour);
  const nearColors: Array<{ rate: number; colors: string[] }> = [];
  const color1 = realtivedColors[0];
  if (color1.diffHour === 0) {
    nearColors.push({ rate: 1, colors: color1.colors });
  } else {
    const color2 = realtivedColors[1];
    const diff12 = diffHour(color1.hour, color2.hour);
    nearColors.push({ ...color1, rate: color1.diffHour / diff12 });
    nearColors.push({ ...color2, rate: color2.diffHour / diff12 });
  }
  return nearColors;
};

const diffHour = (hourA: number, hourB: number) => diffNum(hourA, hourB, 24);

const diffNum = (num1: number, num2: number, cycleSize: number) => {
  const max = Math.max(num1, num2);
  const min = Math.min(num1, num2);
  const diff1 = max - min;
  const diff2 = cycleSize - diff1;
  return Math.min(diff1, diff2);
};
const diffH = (h1: number, h2: number) => diffNum(h1, h2, 360);

const hexToRgb = (hex: string) => {
  hex = hex.slice(1);
  return [parseInt(hex.slice(0, 2), 16), parseInt(hex.slice(2, 4), 16), parseInt(hex.slice(4, 6), 16)] as const;
};

const rgbToHex = (r: number, g: number, b: number) => {
  return "#" + [r, g, b].map((v) => Math.round(v).toString(16).padStart(2, "0")).join("");
};

export const mixColorsByRgb = (
  colors1: string[],
  colors2: string[],
  colors1Rate: number = 0.5,
  colors2Rate: number = 0.5,
) => {
  const rgbColors1 = colors1
    .map((hex) => hexToRgb(hex))
    .map((rgb) => ({ rgb, h: rgbToHsl(...rgb)[0] }))
    .sort((c1, c2) => c1.h - c2.h)
    .map((c) => c.rgb);
  const rgbColors2 = colors2
    .map((hex) => hexToRgb(hex))
    .map((rgb) => ({ rgb, h: rgbToHsl(...rgb)[0] }))
    .sort((c1, c2) => c1.h - c2.h)
    .map((c) => c.rgb);
  const rgbColors3: typeof rgbColors1 = [];
  for (const [index, rgb1] of rgbColors1.entries()) {
    const rgb2 = rgbColors2[index];
    const rgb3 = mixRgbColor(rgb1, rgb2, colors1Rate, colors2Rate);
    rgbColors3.push(rgb3);
  }
  return rgbColors3.map((hsl) => rgbToHex(...hsl));
};
export const mixColorsByHsl = (
  colors1: string[],
  colors2: string[],
  colors1Rate: number = 0.5,
  colors2Rate: number = 0.5,
) => {
  const hslColors1 = colors1.map((hex) => rgbToHsl(...hexToRgb(hex))).sort((hsl1, hsl2) => hsl1[0] - hsl2[0]);
  const hslColors2 = colors2.map((hex) => rgbToHsl(...hexToRgb(hex))).sort((hsl1, hsl2) => hsl1[0] - hsl2[0]);
  const hslColors3: typeof hslColors1 = [];
  for (const [index, hsl1] of hslColors1.entries()) {
    const hsl2 = hslColors2[index];
    const hsl3 = mixHslColor(hsl1, hsl2, colors1Rate, colors2Rate);
    hslColors3.push(hsl3);
  }
  return hslColors3.map((hsl) => rgbToHex(...hslToRgb(...hsl)));
};
type Hsl = readonly [h: number, s: number, l: number];
const mixHslColor = (hsl1: Hsl, hsl2: Hsl, colors1Rate: number = 0.5, colors2Rate: number = 0.5) => {
  let h1 = hsl1[0];
  let h2 = hsl2[0];
  if (h2 + 360 - h1 > 180) {
    [h2, h1] = [h1, h2];
  }
  if (h2 < h1) {
    h2 += 360;
  }
  const h3 = h1 * colors1Rate + h2 * colors2Rate;
  console.log(h1, h2, h3);
  return [
    h3 % 360, //
    Math.min(hsl1[1] * colors1Rate + hsl2[1] * colors2Rate, 100), //
    Math.min(hsl1[2] * colors1Rate + hsl2[2] * colors2Rate, 100), //
  ] as Hsl;
};
const mixRgbColor = (rgb1: Hsl, rgb2: Hsl, colors1Rate: number = 0.5, colors2Rate: number = 0.5) => {
  return [
    Math.min(Math.round(rgb1[0] * colors1Rate + rgb2[0] * colors2Rate), 255), //
    Math.min(Math.round(rgb1[1] * colors1Rate + rgb2[1] * colors2Rate), 255), //
    Math.min(Math.round(rgb1[2] * colors1Rate + rgb2[2] * colors2Rate), 255), //
  ] as Hsl;
};

export const rgbToHsl = (r: number, g: number, b: number) => {
  r /= 255;
  g /= 255;
  b /= 255;

  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  let h: number, s: number;
  const l = (max + min) / 2;

  if (max === min) {
    h = s = 0; // achromatic
  } else {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

    switch (max) {
      case r:
        h = (g - b) / d + (g < b ? 6 : 0);
        break;
      case g:
        h = (b - r) / d + 2;
        break;
      case b:
        h = (r - g) / d + 4;
        break;
      default:
        throw max;
    }

    h /= 6;
  }

  return [h * 360, s * 100, l * 100] as const;
};

export const hslToRgb = (h: number, s: number, l: number) => {
  h /= 360;
  s /= 100;
  l /= 100;

  const c = (1 - Math.abs(2 * l - 1)) * s;
  const x = c * (1 - Math.abs(((h * 6) % 2) - 1));
  const m = l - c / 2;
  let r, g, b;

  if (h < 1 / 6) {
    r = c;
    g = x;
    b = 0;
  } else if (h < 2 / 6) {
    r = x;
    g = c;
    b = 0;
  } else if (h < 3 / 6) {
    r = 0;
    g = c;
    b = x;
  } else if (h < 4 / 6) {
    r = 0;
    g = x;
    b = c;
  } else if (h < 5 / 6) {
    r = x;
    g = 0;
    b = c;
  } else {
    r = c;
    g = 0;
    b = x;
  }

  r = (r + m) * 255;
  g = (g + m) * 255;
  b = (b + m) * 255;

  return [Math.abs(Math.round(r)), Math.abs(Math.round(g)), Math.abs(Math.round(b))] as const;
};

const REFERENCE_COLORS = [
  { hour: 0, colors: ["#e91e63", "#9c27b0"] },
  { hour: 3, colors: ["#ab397d", "#0899f9"] },
  { hour: 6, colors: ["#3f51b5", "#ffeb3b"] },
  { hour: 9, colors: ["#00bcd4", "#03a9f4"] },
  { hour: 12, colors: ["#ffeb3b", "#2196f3"] },
  { hour: 15, colors: ["#f18842", "#44cadc"] },
  { hour: 18, colors: ["#ff9800", "#ff5722"] },
  { hour: 20, colors: ["#ff9800", "#ffeb3b"] },
];
REFERENCE_COLORS.forEach((c) => {
  c.colors = c.colors
    .map((hex) => rgbToHsl(...hexToRgb(hex)))
    .sort((hsl1, hsl2) => hsl1[0] - hsl2[0])
    .map((hsl) => rgbToHex(...hslToRgb(...hsl)));
});
