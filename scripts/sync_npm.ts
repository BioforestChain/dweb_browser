/// 这个代码用来同步到内网
const target = [
  "@dweb-browser/helper",
  "@dweb-browser/core",
  "@dweb-browser/js-process",
  "@plaoc/server",
  "@plaoc/plugins",
  "@plaoc/cli",
  "@plaoc/is-dweb",
  "@dweb-browser/polyfill",
];

const doSync = async () => {
  for (const pack of target) {
    await asyncNpmMirror(pack);
  }
};

const SYNCS_NPM_NIRROR = "https://registry-direct.npmmirror.com/-/package/";
const NPM_NIRROR = "https://registry.npmmirror.com/";
export const asyncNpmMirror = async (name: string) => {
  const path = SYNCS_NPM_NIRROR + `${name}/syncs`;
  const res = await fetch(path, { method: "PUT" });
  const result = await res.json();
  if (result.ok) {
    const response = await fetch(NPM_NIRROR + name);
    const target = (await response.json()) as {
      "dist-tags": {
        latest: string;
      };
    };
    console.log(`✅ ${name} npm_nirror 镜像站同步成功`, `状态：${result.state} => ${target["dist-tags"].latest}`);
  } else {
    console.log("💢npm_nirror 同步失败", result);
  }
};

if (import.meta.main) {
  doSync();
}
