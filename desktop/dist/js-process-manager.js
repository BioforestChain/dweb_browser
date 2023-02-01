"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.JsProcessManager = void 0;
const once_1 = __importDefault(require("lodash/once"));
/**
 * 将指定的js运行在后台的一个管理器，
 * 注意它们共享一个域，所以要么就关闭
 */
class JsProcessManager {
    constructor() {
        this.window = nw.Window.open("../js-process.html");
        this.window;
    }
}
exports.JsProcessManager = JsProcessManager;
JsProcessManager.singleton = (0, once_1.default)(() => new JsProcessManager());
