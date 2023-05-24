"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.MultiWebviewCompVirtualKeyboard = void 0;
const lit_1 = require("lit");
const decorators_js_1 = require("lit/decorators.js");
const repeat_js_1 = require("lit/directives/repeat.js");
const class_map_js_1 = require("lit/directives/class-map.js");
let MultiWebviewCompVirtualKeyboard = class MultiWebviewCompVirtualKeyboard extends lit_1.LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "_elContainer", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_visible", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_overlay", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_navigation_bar_height", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "timer", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "requestId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "insets", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {
                left: 0,
                top: 0,
                right: 0,
                bottom: 0,
            }
        });
        Object.defineProperty(this, "maxHeight", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "row1Keys", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"]
        });
        Object.defineProperty(this, "row2Keys", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ["a", "s", "d", "f", "g", "h", "j", "k", "l"]
        });
        Object.defineProperty(this, "row3Keys", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ["&#8679", "z", "x", "c", "v", "b", "n", "m", "&#10005"]
        });
        Object.defineProperty(this, "row4Keys", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ["123", "&#128512", "space", "search"]
        });
    }
    setHostStyle() {
        const host = this.renderRoot.host;
        host.style.position = this._overlay ? "absolute" : "relative";
        host.style.overflow = this._visible ? "visible" : "hidden";
    }
    firstUpdated() {
        this.setCSSVar();
        this.dispatchEvent(new Event('first-updated'));
    }
    setCSSVar() {
        if (!this._elContainer)
            throw new Error(`this._elContainer === null`);
        const rowWidth = this._elContainer.getBoundingClientRect().width;
        const alphabetWidth = rowWidth / 11;
        const alphabetHeight = alphabetWidth * 1;
        const rowPaddingVertical = 3;
        const rowPaddingHorizontal = 2;
        this.maxHeight = (alphabetHeight + rowPaddingVertical * 2) * 4 + alphabetHeight;
        [
            ["--key-alphabet-width", alphabetWidth],
            ["--key-alphabet-height", alphabetHeight],
            ["--row-padding-vertical", rowPaddingVertical],
            ["--row-padding-horizontal", rowPaddingHorizontal],
            ["--height", this._navigation_bar_height]
        ].forEach(([propertyName, n]) => {
            this._elContainer?.style.setProperty(propertyName, n + 'px');
        });
        return this;
    }
    repeatGetKey(item) {
        return item;
    }
    createElement(classname, key) {
        const div = document.createElement('div');
        div.setAttribute('class', classname);
        div.innerHTML = key;
        return div;
    }
    createElementForRow3(classNameSymbol, classNameAlphabet, key) {
        return this.createElement(key.startsWith("&") ? classNameSymbol : classNameAlphabet, key);
    }
    createElementForRow4(classNameSymbol, classNameSpace, classNameSearch, key) {
        return this.createElement(key.startsWith("1") || key.startsWith("&")
            ? classNameSymbol
            : key === "space"
                ? classNameSpace
                : classNameSearch, key);
    }
    transitionstart() {
        this.timer = setInterval(() => {
            this.dispatchEvent(new Event('height-changed'));
        }, 16);
    }
    transitionend() {
        this.dispatchEvent(new Event(this._visible ? "show-completed" : "hide-completed"));
        clearInterval(this.timer);
        this.dispatchEvent(new Event('height-changed'));
    }
    render() {
        this.setHostStyle();
        const containerClassMap = {
            container: true,
            container_active: this._visible
        };
        return (0, lit_1.html) `
      <div 
        class="${(0, class_map_js_1.classMap)(containerClassMap)}"
        @transitionstart=${this.transitionstart}
        @transitionend=${this.transitionend}  
      >
          <div class="row line-1">
            ${(0, repeat_js_1.repeat)(this.row1Keys, this.repeatGetKey, this.createElement.bind(this, "key-alphabet"))}
          </div>   
          <div class="row line-2">
            ${(0, repeat_js_1.repeat)(this.row2Keys, this.repeatGetKey, this.createElement.bind(this, "key-alphabet"))}
          </div>  
          <div class="row line-3">
            ${(0, repeat_js_1.repeat)(this.row3Keys, this.repeatGetKey, this.createElementForRow3.bind(this, "key-symbol", "key-alphabet"))}
          </div> 
          <div class="row line-4">
            ${(0, repeat_js_1.repeat)(this.row4Keys, this.repeatGetKey, this.createElementForRow4.bind(this, "key-symbol", "key-space", "key-search"))}
          </div> 
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewCompVirtualKeyboard, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    (0, decorators_js_1.query)('.container')
], MultiWebviewCompVirtualKeyboard.prototype, "_elContainer", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Boolean })
], MultiWebviewCompVirtualKeyboard.prototype, "_visible", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Boolean })
], MultiWebviewCompVirtualKeyboard.prototype, "_overlay", void 0);
__decorate([
    (0, decorators_js_1.property)({ type: Number })
], MultiWebviewCompVirtualKeyboard.prototype, "_navigation_bar_height", void 0);
MultiWebviewCompVirtualKeyboard = __decorate([
    (0, decorators_js_1.customElement)('multi-webview-comp-virtual-keyboard')
], MultiWebviewCompVirtualKeyboard);
exports.MultiWebviewCompVirtualKeyboard = MultiWebviewCompVirtualKeyboard;
function createAllCSS() {
    return [
        (0, lit_1.css) `
      :host{
        left: 0px;
        bottom: 0px;
        width: 100%;
      }

      .container{
        --key-alphabet-width: 0px;
        --key-alphabet-height: 0px;
        --row-padding-vertical: 3px;
        --row-padding-horizontal: 2px;
        --border-radius: 3px;
        --height: 0px;
        margin: 0px;
        height: var(--height);
        transition: all 0.25s ease-out;
        overflow: hidden;
        background: #999999;
      }

      .container_active{
        height: calc((var(--key-alphabet-height) + var(--row-padding-vertical) * 2) * 4 + var(--key-alphabet-height));
      }

      .row{
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--row-padding-vertical) var(--row-padding-horizontal);
      }

      .key-alphabet{
        display: flex;
        justify-content: center;
        align-items: center;
        width: var(--key-alphabet-width);
        height: var(--key-alphabet-height);
        border-radius: var(--border-radius);
        background: #fff;
      }

      .line-2{
        padding: var(--row-padding-vertical) calc(var(--row-padding-horizontal) + var(--key-alphabet-width) / 2);
      }

      .key-symbol{
        --margin-horizontal: calc(var(--key-alphabet-width) * 0.3);
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(var(--key-alphabet-width) * 1.2);
        height: var(--key-alphabet-height);
        border-radius: var(--border-radius);
        background: #aaa;
      }

      .key-symbol:first-child{
        margin-right: var(--margin-horizontal);
      }

      .key-symbol:last-child{
        margin-left: var(--margin-horizontal);
      }

      .line-4 .key-symbol:first-child{
        margin-right:0px;
      }

      .line-4 .key-symbol:nth-of-type(2){
        width: calc(var(--key-alphabet-width) * 1.3);
      }

      .key-space{
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: var(--border-radius);
        width: calc(var(--key-alphabet-width) * 6);
        height: var( --key-alphabet-height);
        background: #fff;
      }

      .key-search{
        width: calc(var(--key-alphabet-width) * 2);
        height: var( --key-alphabet-height);
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: var(--border-radius);
        background: #4096ff;
        color: #fff;
      }
    `
    ];
}
