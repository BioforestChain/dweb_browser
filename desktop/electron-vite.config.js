"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const vite_plugin_electron_renderer_1 = __importDefault(require("vite-plugin-electron-renderer"));
exports.default = {
    plugins: [
        (0, vite_plugin_electron_renderer_1.default)( /* options */),
    ],
};
