# Dweb-Browser

<img align="right" src="./assets/browser-icons/web.browser.dweb.svg" height="150px" alt="Cross-platform distributed application browser">

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

dweb-browser is a browser platform built in accordance with Dweb standards, and exposes the capabilities of the browser and the native system capabilities of the browser through various dweb modules. Currently, we have achieved support for mainstream platforms such as Android, IOS, MacOS, Windows, and Linux.

- Download address: [Desktop](https://docs.dweb-browser.org/downloads.html), mobile terminal to download from major app stores.

- Application developer documentation: [docs](https://docs.dweb-browser.org/)

[ENGLISH DOC](./README_EN.md)

# Plaoc

plaoc is a "cross-platform web application" development toolkit based on the dweb-browser platform that is comparable to Cordova, Capacitor, and Tauri, including the packaging tool `@plaoc/cli` and the front-end plug-in `@plaoc/plguins`.

## cli

[@plaoc/cli](https://www.npmjs.com/package/@plaoc/cli) is a command line tool developed by plaoc and packaged to dweb_browser.

### Install the packaging tool.

```bash
npm i -g @plaoc/cli
```

Run ` plaoc serve` when developing an app.

- The first method can specify the output address of your front-end project, so that when your code is updated, the code inside the app will also be updated with your update.

```bash
plaoc serve http://localhost:8000
```

- The second method is to directly specify the compiled source code directory. This method is equivalent to directly installing the app, which is suitable for accessing when your front-end project is disconnected.

```bash
plaoc serve ./dist
```

The above two commands will output the following lines:

```bash
using metadata file: /Volumes/developer/waterbang/deno/dweb_browser/npm/@plaoc__examples/html-demo/manifest.json
metadata: dweb://install?url=http://127.0.0.1:8097/metadata.json
metadata: dweb://install?url=http://172.30.95.93:8097/metadata.json
```

The first line `using metadata file` will specify your app configuration file directory, so you can know which app it is directly.

The second and third lines `metadata` are in the form of `deeplink`, which can be directly pasted into dweb-browser for installation on the desktop.
On the mobile side, you can use the QR code and scan the code to install the application.

### Package into deployable app package

Use `plaoc bundle` to specify the source directory for packaging directly, the command is as follows:

```bash
plaoc bundle ./dir
```

It will package and output a compressed file `.zip` consisting of the app ID and date and a `metadata.json`.

These two files are deployed as links using any `(http/https)` service, placed in the same folder and pointing to the `metadata.json` file. The following form of link can be installed in dweb-browser.

```bash
dweb://install?url=http://app.dweb.中国/metadata.json
```

## plugins

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) can give web developers the ability to directly call the API of each platform system.

For specific documents, please refer to [plugins documentation](https://docs.dweb-browser.org/plugins/web-components.html)

## Module

In dweb-browser, we emphasize the concept of modules. An app can be as small as a simple functional module to focus on providing some capabilities for the distributed network. It includes the following core modules:

### `js.browser.dweb`

It is a javascript-runtime that uses WebWorker as the underlying implementation. Therefore, various standards in WebWorker can be used out of the box.

### `jmm.browser.dweb`

It is a dynamic dweb module manager, based on which PWA-like application functions can be implemented

### `mwebview.browser.dweb`

Its full name is mutil-webview (multi-web view) renderer, which can be used to render multiple web views at the same time. For example, it can be used to implement a web browser.

### `desk.browser.dweb`

It is a window standard `window.std.dweb` implemented by dweb-browser itself, which maintains a consistent window experience across platforms.

### `*.sys.dweb`

Some system standards related to browsers are also implemented on dweb-browser.

### Q&A

If the mac desktop client shows: "Dweb Browser" is damaged and cannot be opened. You should move it to the trash.

You can run it with the following command.

```bash
sudo xattr -d com.apple.quarantine /Applications/Dweb\ Browser.app
```

For more questions, please refer to the document [dweb_browser](https://docs.dweb-browser.org/), or find or ask questions in issuse.
