"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.resolveToRootFile = exports.resolveToRoot = exports.ROOT = exports.createResolveTo = void 0;
const path_1 = __importDefault(require("path"));
const url_1 = require("url");
const createResolveTo = (__dirname) => (...paths) => path_1.default.resolve(__dirname, ...paths);
exports.createResolveTo = createResolveTo;
exports.ROOT = (0, exports.createResolveTo)(__dirname)("../../");
exports.resolveToRoot = (0, exports.createResolveTo)(exports.ROOT);
const resolveToRootFile = (...paths) => (0, url_1.pathToFileURL)((0, exports.resolveToRoot)(...paths));
exports.resolveToRootFile = resolveToRootFile;
