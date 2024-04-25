# About plaoc

[plaoc](https://github.com/BioforestChain/dweb_browser-docs) is a "cross-platform web application" development toolkit based on the dweb-browser platform, aiming to compete with Cordova, Capacitor, and Tauri.

## cli

[plaoc cli documentation](./cli/README.md)

### dev Build

Navigate to the root directory of dweb_browser.

```bash
deno task plaoc serve ./plaoc/demo/dist
```

> `./plaoc/demo/dist` points to the directory of a built application.

## MutilWebview concept

1. In Dweb Browser, MutilWebview serves as a frontend container without polluting global variables. All extensions are implemented through network communication (fetch/websocket).
2. In Dweb Browser, JsProcess serves as a backend container, allowing direct IPC communication with various modules without traditional network layers.
3. The "plugins" folder aggregates and organizes these interfaces within the JsProcess environment, allowing developers to access various modules out of the box. It consists of two parts of code:
   1. Backend code of JsProcess, which accesses other modules through `jsProcess.nativeFetch/.nativeRequest`, allowing developers to mount it to specific subdomains/ports, enabling frontend developers to access these modules.
   2. Frontend code of MutilWebview, which further encapsulates the backend's network requests into declarative WebComponents, making the interfaces more intuitive for frontend developers.
      > Additionally, we will further encapsulate these WebComponents to make them compatible with traditional frontend application development frameworks like Capacitor and Cordova.

## publish

- Tag and push to GitHub.

```bash
git tag -a 0.0.1 -m "feat: xxx"
```

```bash
git push origin <tag-name>
```