import { assert, test } from "vitest";
import { findNearColors, getDayHoursConfig, hslToRgb, mixColorsByHsl, rgbToHsl } from "./color-generator.ts";
test("findNearColors", () => {
  console.log(0, findNearColors(0));
  console.log(1, findNearColors(1));
  console.log(23, findNearColors(23));
});
test("rgbToHsl", () => {
  for (let i = 0; i < 10000; i++) {
    const r = Math.ceil(Math.random() * 256) % 256;
    const g = Math.ceil(Math.random() * 256) % 256;
    const b = Math.ceil(Math.random() * 256) % 256;
    assert.deepEqual(hslToRgb(...rgbToHsl(r, g, b)), [r, g, b]);
  }
});
test("getDayHoursConfig", () => {
  const config = getDayHoursConfig();
  console.log(config + "\n");
  console.log(
    config
      .split("\n")
      .map(
        (line, i) =>
          `background: #${i.toString().padStart(3, "0")} linear-gradient(90deg, ${line.split("overlay")[1].trim().replace(" ", ", ")});`,
      )
      .join("\n"),
  );
});

test("mixColorsByHsl", () => {
  const nearColors = [
    {
      hour: 20,
      colors: ["#ff9800", "#ffeb3b"],
      diffHour: 1,
      rate: 0.25,
    },
    {
      hour: 0,
      colors: ["#9c27b0", "#e91e63"],
      diffHour: 3,
      rate: 0.75,
    },
  ];
  const hsl = mixColorsByHsl(nearColors[0].colors, nearColors[1].colors, nearColors[0].rate, nearColors[1].rate);
  console.log(hsl);
});
