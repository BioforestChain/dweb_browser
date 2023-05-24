export const setChromeProxy = async (port: number, host = "localhost") => {
  const pac_script_config = {
    mode: "pac_script",
    pacScript: {
      data: `function FindProxyForURL(url,host){
          return 'HTTPS ${host}:${port}';
        }`,
    },
  };
  await new Promise<void>((resolve, reject) => {
    chrome.proxy.settings.set(
      { value: pac_script_config, scope: "regular" },
      resolve
    );
  });
  return () =>
    new Promise<void>((resolve, reject) => {
      chrome.proxy.settings.clear({ scope: "regular" }, resolve);
    });
};
