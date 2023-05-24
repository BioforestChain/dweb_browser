"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const electron_1 = require("electron");
try {
    electron_1.protocol.registerSchemesAsPrivileged([
        { scheme: 'http', privileges: { bypassCSP: true, standard: true, secure: true, stream: true } },
        { scheme: 'https', privileges: { bypassCSP: true, standard: true, secure: true, stream: true } },
    ]);
}
catch (err) {
    console.log('err: ', err);
}
const main_js_1 = require("./main.js");
main_js_1.dns.bootstrap();
