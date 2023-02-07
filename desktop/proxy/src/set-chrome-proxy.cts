export const setChromeProxy = (port = 22600, host = "localhost") => {
  const fixed_servers_config = {
    mode: "fixed_servers",
    rules: {
      proxyForHttp: {
        scheme: "https",
        host: host,
        port: port,
      },
      proxyForHttps: {
        scheme: "https",
        host: host,
        port: port,
      },
      //   bypassList: ["google.com"],
    },
  };
  const pac_script_config = {
    mode: "pac_script",
    pacScript: {
      data: `function FindProxyForURL(url,host){
        return 'HTTPS ${host}:${port}';
      }`,
    },
  };
  chrome.proxy.settings.set(
    { value: pac_script_config, scope: "regular" },
    function () {}
  );
};
