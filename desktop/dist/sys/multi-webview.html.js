"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ViewTree = void 0;
const lit_1 = require("lit");
const decorators_js_1 = require("lit/decorators.js");
let ViewTree = class ViewTree extends lit_1.LitElement {
    constructor() {
        super(...arguments);
        // Declare reactive properties
        this.name = "World";
    }
    // Render the UI as a function of component state
    render() {
        return (0, lit_1.html) `<p>Hello, ${this.name}!</p>`;
    }
};
// Define scoped styles right with your component, in plain CSS
ViewTree.styles = (0, lit_1.css) `
      :host {
         color: blue;
      }
   `;
__decorate([
    (0, decorators_js_1.property)(),
    __metadata("design:type", String)
], ViewTree.prototype, "name", void 0);
ViewTree = __decorate([
    (0, decorators_js_1.customElement)("view-tree")
], ViewTree);
exports.ViewTree = ViewTree;
