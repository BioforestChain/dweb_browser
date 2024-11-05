# Dweb Browser

<img align="right" src="./assets/browser-icons/web.browser.dweb.svg" height="150px" alt="Cross-platform distributed application browser">

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

[ZH DOC](./README_ZH.md) ｜ [Welcome to Dweb Browser discord](https://discord.gg/nBPgPzPbgX)

dweb-browser is a browser platform built in accordance with Dweb standards, and exposes the capabilities of the browser and the native system capabilities of the browser through various dweb modules. Currently, we have implemented support for mainstream platforms such as Android, iOS, MacOS, and Windows.

We define the app installed in `Dweb Browser` as a `dweb app`. With a simple configuration file, you can quickly install your `web app` on various platforms.

How to start development: [flow](https://docs.dweb-browser.org/plaoc/flow.html).

> - `dweb app` application developer documentation: [docs](https://docs.dweb-browser.org/)

> - [Desktop download address](https://docs.dweb-browser.org/downloads.html) Please download the version of the corresponding platform.

> - Search for `Dweb Browser` in major app stores on mobile devices to download and use.

## `dweb`

In `Dweb Browser`, we emphasize the concept of modules, and all functions are `dweb` modules.

A `dweb app` can be as small as a simple functional module to focus on providing some capabilities for the distributed network, that is, the module you call locally can be a remote module.

The concept of modularity will be further reflected in the programmable backend. The concept of modules is weakened for front-end developers, and front-end developers simply call any plug-in for any function.

Provide multi-level development requirements.

The module includes `std.dweb`, `sys.dweb`, `browser.dweb`, `*.dweb` and other standards.

### `std.dweb` standard

The full-platform standard module, the interface provided needs to be consistent on each platform. And it needs to be recognized by the community.

Currently there are three:

- `file.std.dweb`: a standard module for file operations, providing standard capabilities for operating folders.

- `http.std.dweb`: a standard module for http services.

- `dns.std.dweb`: dns forwarding lookup module.

### `sys.dweb` standard

System-level module, providing some system-level capabilities, the capabilities of each platform may be inconsistent.
When some modules have consistent interfaces across all platforms and are recognized by the community, they will be upgraded to standard modules across all platforms.

Some examples:

- `haptics.sys.dweb`: vibration module, providing vibration capabilities, only implemented on mobile terminals.

- `keychain.sys.dweb`: key storage module.

- `geolocation.sys.dweb`: module for obtaining longitude and latitude.

- ...

### `browser.dweb` standard

Browser-level module, implementing some browser-level functions.

Some examples:

- `download.browser.dweb`: download module, providing download capabilities.

- `jmm.browser.dweb`: It is a dynamic dweb module manager, based on which application functions similar to PWA can be implemented.
- `js.browser.dweb`: It is a javascript-runtime that uses WebWorker as the underlying implementation. Therefore, various standards in WebWorker can be used out of the box.
- ...

# Plaoc

plaoc is a "cross-platform web application" development toolkit based on the `Dweb Browser` platform that is comparable to Cordova, Capacitor, and Tauri.

It provides full-link tools and can quickly call the capabilities of various platforms and systems.

Contains the packaging tool `@plaoc/cli` and the front-end plug-in `@plaoc/plugins`.

## `@plaoc/cli`

[@plaoc/cli](https://www.npmjs.com/package/@plaoc/cli) is a command line tool for `dweb app` development and packaging applications to `Dweb Browser`.

For specific documents, please refer to [cli document](https://docs.dweb-browser.org/plaoc/cli.html)

## `@plaoc/plugins`

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) can give web developers the ability to directly call the API of each platform system.

For specific documents, please refer to [plugins document](https://docs.dweb-browser.org/plugins/web-components.html)

## How to contribute code

1. View: [Quick Start](./GET_START_FOR_DEVELOPER.quick.en.md)
2. Recommended reading: [Complete development environment setup documentation](./GET_START_FOR_DEVELOPER.en.md)
3. Initialize resource package

```bash
deno task init
```

### Contribute to multi-platform applications

Need to compile `Dweb Browser` static resource files.

```bash
deno task dev
```

### Contribute to the `Plaoc` toolkit

```bash
deno task plaoc:watch
```

### Example project

The project contains front-end and back-end example projects, located in [examples](./toolkit/plaoc/examples/).

You can run the following command to view:

#### Front-end example project

```bash
plaoc:demo
```

#### Contains a programmable back-end example project

```bash
plaoc:demo:serve
```

### Contribute to static libraries

Some system-level APIs and services of the project are called using [rust-uniffi](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings).

See the project for details [dweb_browser_libs](https://github.com/BioforestChain/dweb_browser_libs).
