import translate from "npm:translate";

Deno.test("测试翻译效果", async () => {
  const text = await translate("接天莲叶无穷碧，映日荷花别样红。", { from: "zh", to: "en" });
  console.log(text);
});
