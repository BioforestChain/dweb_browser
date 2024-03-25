<p align="center">
<!-- <h1>Dweb-Browser</h1> -->
<img src="./desktop-dev/logo.png" width="240">
</p>

[CHINESE DOC](./README.md.md)
[DEVELOPER DOC](./GET_START_FOR_DEVELOPER.md)

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

# About dweb-browser

The dweb-browser is a browser platform built following the Dweb standards, exposing browser capabilities and native system capabilities related to browsers through various dweb modules. Currently, we have implemented support for mainstream platforms such as Android, iOS, MacOS, Windows, and Linux.
It includes the following core modules:

### `js.browser.dweb`

It is a JavaScript runtime that uses WebWorkers as the underlying implementation. Therefore, various standards in WebWorkers can be used out of the box.

### `jmm.browser.dweb`

It is a dynamic dweb module manager that enables the implementation of application functionality similar to PWA.

### `mwebview.browser.dweb`

It is a renderer called mutil-webview (multiple web views), which can render multiple web views simultaneously. For example, it can be used to implement a web browser.

### `desk.browser.dweb`

It is a dweb-browser's own implementation of the window standard `window.std.dweb` to maintain a consistent window experience across platforms.

### `*.sys.dweb`

Some system standards related to browsers are also implemented in dweb-browser.

## What is plaoc

plaoc is a "cross-platform web application" development toolkit based on the dweb-browser platform, similar to Cordova, Capacitor, and Tauri.

### cli

[@plaoc/cli](https://www.npmjs.com/package/@plaoc/cli) is the command-line tool developed by plaoc for developing and packaging applications for dweb_browser.

1. `npm i -g @plaoc/cli`

2. `plaoc bundle ./dir`
   It will create the following folder structure and output a compressed file `.zip` and a `plaoc.metadata.json`.

3. `plaoc preview http://localhost:1231` or `plaoc preview ./dir`
   > This will preview the URL in an iframe.
   > This command will output a command line:
   ```bash
   dweb-browser-dev install --url http://172.30.90.240:8080/usr/metadata.json
   ```

### plugins

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) provides web developers with the ability to directly call system APIs of various platforms.

For detailed documentation, please refer to: [plugins documentation](https://docs.dweb-browser.org/plugins/web-components.html)
