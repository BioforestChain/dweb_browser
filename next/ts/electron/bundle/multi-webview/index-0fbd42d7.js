(function(){const e=document.createElement("link").relList;if(e&&e.supports&&e.supports("modulepreload"))return;for(const r of document.querySelectorAll('link[rel="modulepreload"]'))s(r);new MutationObserver(r=>{for(const n of r)if(n.type==="childList")for(const o of n.addedNodes)o.tagName==="LINK"&&o.rel==="modulepreload"&&s(o)}).observe(document,{childList:!0,subtree:!0});function i(r){const n={};return r.integrity&&(n.integrity=r.integrity),r.referrerPolicy&&(n.referrerPolicy=r.referrerPolicy),r.crossOrigin==="use-credentials"?n.credentials="include":r.crossOrigin==="anonymous"?n.credentials="omit":n.credentials="same-origin",n}function s(r){if(r.ep)return;r.ep=!0;const n=i(r);fetch(r.href,n)}})();/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const de=window,ze=de.ShadowRoot&&(de.ShadyCSS===void 0||de.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,Ne=Symbol(),Ze=new WeakMap;let vt=class{constructor(e,i,s){if(this._$cssResult$=!0,s!==Ne)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=e,this.t=i}get styleSheet(){let e=this.o;const i=this.t;if(ze&&e===void 0){const s=i!==void 0&&i.length===1;s&&(e=Ze.get(i)),e===void 0&&((this.o=e=new CSSStyleSheet).replaceSync(this.cssText),s&&Ze.set(i,e))}return e}toString(){return this.cssText}};const Zt=t=>new vt(typeof t=="string"?t:t+"",void 0,Ne),$=(t,...e)=>{const i=t.length===1?t[0]:e.reduce((s,r,n)=>s+(o=>{if(o._$cssResult$===!0)return o.cssText;if(typeof o=="number")return o;throw Error("Value passed to 'css' function must be a 'css' function result: "+o+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(r)+t[n+1],t[0]);return new vt(i,t,Ne)},Yt=(t,e)=>{ze?t.adoptedStyleSheets=e.map(i=>i instanceof CSSStyleSheet?i:i.styleSheet):e.forEach(i=>{const s=document.createElement("style"),r=de.litNonce;r!==void 0&&s.setAttribute("nonce",r),s.textContent=i.cssText,t.appendChild(s)})},Ye=ze?t=>t:t=>t instanceof CSSStyleSheet?(e=>{let i="";for(const s of e.cssRules)i+=s.cssText;return Zt(i)})(t):t;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var Ce;const be=window,Xe=be.trustedTypes,Xt=Xe?Xe.emptyScript:"",Qe=be.reactiveElementPolyfillSupport,We={toAttribute(t,e){switch(e){case Boolean:t=t?Xt:null;break;case Object:case Array:t=t==null?t:JSON.stringify(t)}return t},fromAttribute(t,e){let i=t;switch(e){case Boolean:i=t!==null;break;case Number:i=t===null?null:Number(t);break;case Object:case Array:try{i=JSON.parse(t)}catch{i=null}}return i}},bt=(t,e)=>e!==t&&(e==e||t==t),Fe={attribute:!0,type:String,converter:We,reflect:!1,hasChanged:bt};let Y=class extends HTMLElement{constructor(){super(),this._$Ei=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this._$El=null,this.u()}static addInitializer(e){var i;this.finalize(),((i=this.h)!==null&&i!==void 0?i:this.h=[]).push(e)}static get observedAttributes(){this.finalize();const e=[];return this.elementProperties.forEach((i,s)=>{const r=this._$Ep(s,i);r!==void 0&&(this._$Ev.set(r,s),e.push(r))}),e}static createProperty(e,i=Fe){if(i.state&&(i.attribute=!1),this.finalize(),this.elementProperties.set(e,i),!i.noAccessor&&!this.prototype.hasOwnProperty(e)){const s=typeof e=="symbol"?Symbol():"__"+e,r=this.getPropertyDescriptor(e,s,i);r!==void 0&&Object.defineProperty(this.prototype,e,r)}}static getPropertyDescriptor(e,i,s){return{get(){return this[i]},set(r){const n=this[e];this[i]=r,this.requestUpdate(e,n,s)},configurable:!0,enumerable:!0}}static getPropertyOptions(e){return this.elementProperties.get(e)||Fe}static finalize(){if(this.hasOwnProperty("finalized"))return!1;this.finalized=!0;const e=Object.getPrototypeOf(this);if(e.finalize(),e.h!==void 0&&(this.h=[...e.h]),this.elementProperties=new Map(e.elementProperties),this._$Ev=new Map,this.hasOwnProperty("properties")){const i=this.properties,s=[...Object.getOwnPropertyNames(i),...Object.getOwnPropertySymbols(i)];for(const r of s)this.createProperty(r,i[r])}return this.elementStyles=this.finalizeStyles(this.styles),!0}static finalizeStyles(e){const i=[];if(Array.isArray(e)){const s=new Set(e.flat(1/0).reverse());for(const r of s)i.unshift(Ye(r))}else e!==void 0&&i.push(Ye(e));return i}static _$Ep(e,i){const s=i.attribute;return s===!1?void 0:typeof s=="string"?s:typeof e=="string"?e.toLowerCase():void 0}u(){var e;this._$E_=new Promise(i=>this.enableUpdating=i),this._$AL=new Map,this._$Eg(),this.requestUpdate(),(e=this.constructor.h)===null||e===void 0||e.forEach(i=>i(this))}addController(e){var i,s;((i=this._$ES)!==null&&i!==void 0?i:this._$ES=[]).push(e),this.renderRoot!==void 0&&this.isConnected&&((s=e.hostConnected)===null||s===void 0||s.call(e))}removeController(e){var i;(i=this._$ES)===null||i===void 0||i.splice(this._$ES.indexOf(e)>>>0,1)}_$Eg(){this.constructor.elementProperties.forEach((e,i)=>{this.hasOwnProperty(i)&&(this._$Ei.set(i,this[i]),delete this[i])})}createRenderRoot(){var e;const i=(e=this.shadowRoot)!==null&&e!==void 0?e:this.attachShadow(this.constructor.shadowRootOptions);return Yt(i,this.constructor.elementStyles),i}connectedCallback(){var e;this.renderRoot===void 0&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),(e=this._$ES)===null||e===void 0||e.forEach(i=>{var s;return(s=i.hostConnected)===null||s===void 0?void 0:s.call(i)})}enableUpdating(e){}disconnectedCallback(){var e;(e=this._$ES)===null||e===void 0||e.forEach(i=>{var s;return(s=i.hostDisconnected)===null||s===void 0?void 0:s.call(i)})}attributeChangedCallback(e,i,s){this._$AK(e,s)}_$EO(e,i,s=Fe){var r;const n=this.constructor._$Ep(e,s);if(n!==void 0&&s.reflect===!0){const o=(((r=s.converter)===null||r===void 0?void 0:r.toAttribute)!==void 0?s.converter:We).toAttribute(i,s.type);this._$El=e,o==null?this.removeAttribute(n):this.setAttribute(n,o),this._$El=null}}_$AK(e,i){var s;const r=this.constructor,n=r._$Ev.get(e);if(n!==void 0&&this._$El!==n){const o=r.getPropertyOptions(n),l=typeof o.converter=="function"?{fromAttribute:o.converter}:((s=o.converter)===null||s===void 0?void 0:s.fromAttribute)!==void 0?o.converter:We;this._$El=n,this[n]=l.fromAttribute(i,o.type),this._$El=null}}requestUpdate(e,i,s){let r=!0;e!==void 0&&(((s=s||this.constructor.getPropertyOptions(e)).hasChanged||bt)(this[e],i)?(this._$AL.has(e)||this._$AL.set(e,i),s.reflect===!0&&this._$El!==e&&(this._$EC===void 0&&(this._$EC=new Map),this._$EC.set(e,s))):r=!1),!this.isUpdatePending&&r&&(this._$E_=this._$Ej())}async _$Ej(){this.isUpdatePending=!0;try{await this._$E_}catch(i){Promise.reject(i)}const e=this.scheduleUpdate();return e!=null&&await e,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var e;if(!this.isUpdatePending)return;this.hasUpdated,this._$Ei&&(this._$Ei.forEach((r,n)=>this[n]=r),this._$Ei=void 0);let i=!1;const s=this._$AL;try{i=this.shouldUpdate(s),i?(this.willUpdate(s),(e=this._$ES)===null||e===void 0||e.forEach(r=>{var n;return(n=r.hostUpdate)===null||n===void 0?void 0:n.call(r)}),this.update(s)):this._$Ek()}catch(r){throw i=!1,this._$Ek(),r}i&&this._$AE(s)}willUpdate(e){}_$AE(e){var i;(i=this._$ES)===null||i===void 0||i.forEach(s=>{var r;return(r=s.hostUpdated)===null||r===void 0?void 0:r.call(s)}),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(e)),this.updated(e)}_$Ek(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$E_}shouldUpdate(e){return!0}update(e){this._$EC!==void 0&&(this._$EC.forEach((i,s)=>this._$EO(s,this[s],i)),this._$EC=void 0),this._$Ek()}updated(e){}firstUpdated(e){}};Y.finalized=!0,Y.elementProperties=new Map,Y.elementStyles=[],Y.shadowRootOptions={mode:"open"},Qe==null||Qe({ReactiveElement:Y}),((Ce=be.reactiveElementVersions)!==null&&Ce!==void 0?Ce:be.reactiveElementVersions=[]).push("1.6.1");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var Oe;const fe=window,Q=fe.trustedTypes,et=Q?Q.createPolicy("lit-html",{createHTML:t=>t}):void 0,ge="$lit$",k=`lit$${(Math.random()+"").slice(9)}$`,Ue="?"+k,Qt=`<${Ue}>`,U=document,se=()=>U.createComment(""),re=t=>t===null||typeof t!="object"&&typeof t!="function",ft=Array.isArray,gt=t=>ft(t)||typeof(t==null?void 0:t[Symbol.iterator])=="function",ke=`[ 	
\f\r]`,te=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,tt=/-->/g,it=/>/g,L=RegExp(`>|${ke}(?:([^\\s"'>=/]+)(${ke}*=${ke}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),st=/'/g,rt=/"/g,yt=/^(?:script|style|textarea|title)$/i,ei=t=>(e,...i)=>({_$litType$:t,strings:e,values:i}),f=ei(1),P=Symbol.for("lit-noChange"),y=Symbol.for("lit-nothing"),nt=new WeakMap,N=U.createTreeWalker(U,129,null,!1),wt=(t,e)=>{const i=t.length-1,s=[];let r,n=e===2?"<svg>":"",o=te;for(let c=0;c<i;c++){const a=t[c];let v,p,h=-1,b=0;for(;b<a.length&&(o.lastIndex=b,p=o.exec(a),p!==null);)b=o.lastIndex,o===te?p[1]==="!--"?o=tt:p[1]!==void 0?o=it:p[2]!==void 0?(yt.test(p[2])&&(r=RegExp("</"+p[2],"g")),o=L):p[3]!==void 0&&(o=L):o===L?p[0]===">"?(o=r??te,h=-1):p[1]===void 0?h=-2:(h=o.lastIndex-p[2].length,v=p[1],o=p[3]===void 0?L:p[3]==='"'?rt:st):o===rt||o===st?o=L:o===tt||o===it?o=te:(o=L,r=void 0);const g=o===L&&t[c+1].startsWith("/>")?" ":"";n+=o===te?a+Qt:h>=0?(s.push(v),a.slice(0,h)+ge+a.slice(h)+k+g):a+k+(h===-2?(s.push(void 0),c):g)}const l=n+(t[i]||"<?>")+(e===2?"</svg>":"");if(!Array.isArray(t)||!t.hasOwnProperty("raw"))throw Error("invalid template strings array");return[et!==void 0?et.createHTML(l):l,s]};class ne{constructor({strings:e,_$litType$:i},s){let r;this.parts=[];let n=0,o=0;const l=e.length-1,c=this.parts,[a,v]=wt(e,i);if(this.el=ne.createElement(a,s),N.currentNode=this.el.content,i===2){const p=this.el.content,h=p.firstChild;h.remove(),p.append(...h.childNodes)}for(;(r=N.nextNode())!==null&&c.length<l;){if(r.nodeType===1){if(r.hasAttributes()){const p=[];for(const h of r.getAttributeNames())if(h.endsWith(ge)||h.startsWith(k)){const b=v[o++];if(p.push(h),b!==void 0){const g=r.getAttribute(b.toLowerCase()+ge).split(k),w=/([.?@])?(.*)/.exec(b);c.push({type:1,index:n,name:w[2],strings:g,ctor:w[1]==="."?mt:w[1]==="?"?$t:w[1]==="@"?St:oe})}else c.push({type:6,index:n})}for(const h of p)r.removeAttribute(h)}if(yt.test(r.tagName)){const p=r.textContent.split(k),h=p.length-1;if(h>0){r.textContent=Q?Q.emptyScript:"";for(let b=0;b<h;b++)r.append(p[b],se()),N.nextNode(),c.push({type:2,index:++n});r.append(p[h],se())}}}else if(r.nodeType===8)if(r.data===Ue)c.push({type:2,index:n});else{let p=-1;for(;(p=r.data.indexOf(k,p+1))!==-1;)c.push({type:7,index:n}),p+=k.length-1}n++}}static createElement(e,i){const s=U.createElement("template");return s.innerHTML=e,s}}function K(t,e,i=t,s){var r,n,o,l;if(e===P)return e;let c=s!==void 0?(r=i._$Co)===null||r===void 0?void 0:r[s]:i._$Cl;const a=re(e)?void 0:e._$litDirective$;return(c==null?void 0:c.constructor)!==a&&((n=c==null?void 0:c._$AO)===null||n===void 0||n.call(c,!1),a===void 0?c=void 0:(c=new a(t),c._$AT(t,i,s)),s!==void 0?((o=(l=i)._$Co)!==null&&o!==void 0?o:l._$Co=[])[s]=c:i._$Cl=c),c!==void 0&&(e=K(t,c._$AS(t,e.values),c,s)),e}class _t{constructor(e,i){this._$AV=[],this._$AN=void 0,this._$AD=e,this._$AM=i}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(e){var i;const{el:{content:s},parts:r}=this._$AD,n=((i=e==null?void 0:e.creationScope)!==null&&i!==void 0?i:U).importNode(s,!0);N.currentNode=n;let o=N.nextNode(),l=0,c=0,a=r[0];for(;a!==void 0;){if(l===a.index){let v;a.type===2?v=new ee(o,o.nextSibling,this,e):a.type===1?v=new a.ctor(o,a.name,a.strings,this,e):a.type===6&&(v=new xt(o,this,e)),this._$AV.push(v),a=r[++c]}l!==(a==null?void 0:a.index)&&(o=N.nextNode(),l++)}return N.currentNode=U,n}v(e){let i=0;for(const s of this._$AV)s!==void 0&&(s.strings!==void 0?(s._$AI(e,s,i),i+=s.strings.length-2):s._$AI(e[i])),i++}}class ee{constructor(e,i,s,r){var n;this.type=2,this._$AH=y,this._$AN=void 0,this._$AA=e,this._$AB=i,this._$AM=s,this.options=r,this._$Cp=(n=r==null?void 0:r.isConnected)===null||n===void 0||n}get _$AU(){var e,i;return(i=(e=this._$AM)===null||e===void 0?void 0:e._$AU)!==null&&i!==void 0?i:this._$Cp}get parentNode(){let e=this._$AA.parentNode;const i=this._$AM;return i!==void 0&&(e==null?void 0:e.nodeType)===11&&(e=i.parentNode),e}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(e,i=this){e=K(this,e,i),re(e)?e===y||e==null||e===""?(this._$AH!==y&&this._$AR(),this._$AH=y):e!==this._$AH&&e!==P&&this._(e):e._$litType$!==void 0?this.g(e):e.nodeType!==void 0?this.$(e):gt(e)?this.T(e):this._(e)}k(e){return this._$AA.parentNode.insertBefore(e,this._$AB)}$(e){this._$AH!==e&&(this._$AR(),this._$AH=this.k(e))}_(e){this._$AH!==y&&re(this._$AH)?this._$AA.nextSibling.data=e:this.$(U.createTextNode(e)),this._$AH=e}g(e){var i;const{values:s,_$litType$:r}=e,n=typeof r=="number"?this._$AC(e):(r.el===void 0&&(r.el=ne.createElement(r.h,this.options)),r);if(((i=this._$AH)===null||i===void 0?void 0:i._$AD)===n)this._$AH.v(s);else{const o=new _t(n,this),l=o.u(this.options);o.v(s),this.$(l),this._$AH=o}}_$AC(e){let i=nt.get(e.strings);return i===void 0&&nt.set(e.strings,i=new ne(e)),i}T(e){ft(this._$AH)||(this._$AH=[],this._$AR());const i=this._$AH;let s,r=0;for(const n of e)r===i.length?i.push(s=new ee(this.k(se()),this.k(se()),this,this.options)):s=i[r],s._$AI(n),r++;r<i.length&&(this._$AR(s&&s._$AB.nextSibling,r),i.length=r)}_$AR(e=this._$AA.nextSibling,i){var s;for((s=this._$AP)===null||s===void 0||s.call(this,!1,!0,i);e&&e!==this._$AB;){const r=e.nextSibling;e.remove(),e=r}}setConnected(e){var i;this._$AM===void 0&&(this._$Cp=e,(i=this._$AP)===null||i===void 0||i.call(this,e))}}class oe{constructor(e,i,s,r,n){this.type=1,this._$AH=y,this._$AN=void 0,this.element=e,this.name=i,this._$AM=r,this.options=n,s.length>2||s[0]!==""||s[1]!==""?(this._$AH=Array(s.length-1).fill(new String),this.strings=s):this._$AH=y}get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}_$AI(e,i=this,s,r){const n=this.strings;let o=!1;if(n===void 0)e=K(this,e,i,0),o=!re(e)||e!==this._$AH&&e!==P,o&&(this._$AH=e);else{const l=e;let c,a;for(e=n[0],c=0;c<n.length-1;c++)a=K(this,l[s+c],i,c),a===P&&(a=this._$AH[c]),o||(o=!re(a)||a!==this._$AH[c]),a===y?e=y:e!==y&&(e+=(a??"")+n[c+1]),this._$AH[c]=a}o&&!r&&this.j(e)}j(e){e===y?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,e??"")}}class mt extends oe{constructor(){super(...arguments),this.type=3}j(e){this.element[this.name]=e===y?void 0:e}}const ti=Q?Q.emptyScript:"";class $t extends oe{constructor(){super(...arguments),this.type=4}j(e){e&&e!==y?this.element.setAttribute(this.name,ti):this.element.removeAttribute(this.name)}}class St extends oe{constructor(e,i,s,r,n){super(e,i,s,r,n),this.type=5}_$AI(e,i=this){var s;if((e=(s=K(this,e,i,0))!==null&&s!==void 0?s:y)===P)return;const r=this._$AH,n=e===y&&r!==y||e.capture!==r.capture||e.once!==r.once||e.passive!==r.passive,o=e!==y&&(r===y||n);n&&this.element.removeEventListener(this.name,this,r),o&&this.element.addEventListener(this.name,this,e),this._$AH=e}handleEvent(e){var i,s;typeof this._$AH=="function"?this._$AH.call((s=(i=this.options)===null||i===void 0?void 0:i.host)!==null&&s!==void 0?s:this.element,e):this._$AH.handleEvent(e)}}class xt{constructor(e,i,s){this.element=e,this.type=6,this._$AN=void 0,this._$AM=i,this.options=s}get _$AU(){return this._$AM._$AU}_$AI(e){K(this,e)}}const ii={O:ge,P:k,A:Ue,C:1,M:wt,L:_t,D:gt,R:K,I:ee,V:oe,H:$t,N:St,U:mt,F:xt},ot=fe.litHtmlPolyfillSupport;ot==null||ot(ne,ee),((Oe=fe.litHtmlVersions)!==null&&Oe!==void 0?Oe:fe.litHtmlVersions=[]).push("2.7.4");const si=(t,e,i)=>{var s,r;const n=(s=i==null?void 0:i.renderBefore)!==null&&s!==void 0?s:e;let o=n._$litPart$;if(o===void 0){const l=(r=i==null?void 0:i.renderBefore)!==null&&r!==void 0?r:null;n._$litPart$=o=new ee(e.insertBefore(se(),l),l,void 0,i??{})}return o._$AI(t),o};/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var Me,Pe;let _=class extends Y{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){var e,i;const s=super.createRenderRoot();return(e=(i=this.renderOptions).renderBefore)!==null&&e!==void 0||(i.renderBefore=s.firstChild),s}update(e){const i=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(e),this._$Do=si(i,this.renderRoot,this.renderOptions)}connectedCallback(){var e;super.connectedCallback(),(e=this._$Do)===null||e===void 0||e.setConnected(!0)}disconnectedCallback(){var e;super.disconnectedCallback(),(e=this._$Do)===null||e===void 0||e.setConnected(!1)}render(){return P}};_.finalized=!0,_._$litElement$=!0,(Me=globalThis.litElementHydrateSupport)===null||Me===void 0||Me.call(globalThis,{LitElement:_});const at=globalThis.litElementPolyfillSupport;at==null||at({LitElement:_});((Pe=globalThis.litElementVersions)!==null&&Pe!==void 0?Pe:globalThis.litElementVersions=[]).push("3.3.2");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const A=t=>e=>typeof e=="function"?((i,s)=>(customElements.define(i,s),s))(t,e):((i,s)=>{const{kind:r,elements:n}=s;return{kind:r,elements:n,finisher(o){customElements.define(i,o)}}})(t,e);/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const ri=(t,e)=>e.kind==="method"&&e.descriptor&&!("value"in e.descriptor)?{...e,finisher(i){i.createProperty(e.key,t)}}:{kind:"field",key:Symbol(),placement:"own",descriptor:{},originalKey:e.key,initializer(){typeof e.initializer=="function"&&(this[e.key]=e.initializer.call(this))},finisher(i){i.createProperty(e.key,t)}};function d(t){return(e,i)=>i!==void 0?((s,r,n)=>{r.constructor.createProperty(n,s)})(t,e,i):ri(t,e)}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function Ae(t){return d({...t,state:!0})}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const At=({finisher:t,descriptor:e})=>(i,s)=>{var r;if(s===void 0){const n=(r=i.originalKey)!==null&&r!==void 0?r:i.key,o=e!=null?{kind:"method",placement:"prototype",key:n,descriptor:e(i.key)}:{...i,key:n};return t!=null&&(o.finisher=function(l){t(l,n)}),o}{const n=i.constructor;e!==void 0&&Object.defineProperty(i,s,e(s)),t==null||t(n,s)}};/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function G(t,e){return At({descriptor:i=>{const s={get(){var r,n;return(n=(r=this.renderRoot)===null||r===void 0?void 0:r.querySelector(t))!==null&&n!==void 0?n:null},enumerable:!0,configurable:!0};if(e){const r=typeof i=="symbol"?Symbol():"__"+i;s.get=function(){var n,o;return this[r]===void 0&&(this[r]=(o=(n=this.renderRoot)===null||n===void 0?void 0:n.querySelector(t))!==null&&o!==void 0?o:null),this[r]}}return s}})}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function ni(t){return At({descriptor:e=>({get(){var i,s;return(s=(i=this.renderRoot)===null||i===void 0?void 0:i.querySelectorAll(t))!==null&&s!==void 0?s:[]},enumerable:!0,configurable:!0})})}/**
 * @license
 * Copyright 2021 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var Be;((Be=window.HTMLSlotElement)===null||Be===void 0?void 0:Be.prototype.assignedElements)!=null;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const Ke={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},De=t=>(...e)=>({_$litDirective$:t,values:e});let Ve=class{constructor(e){}get _$AU(){return this._$AM._$AU}_$AT(e,i,s){this._$Ct=e,this._$AM=i,this._$Ci=s}_$AS(e,i){return this.update(e,i)}update(e,i){return this.render(...i)}};/**
 * @license
 * Copyright 2018 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const Et="important",oi=" !"+Et,M=De(class extends Ve{constructor(t){var e;if(super(t),t.type!==Ke.ATTRIBUTE||t.name!=="style"||((e=t.strings)===null||e===void 0?void 0:e.length)>2)throw Error("The `styleMap` directive must be used in the `style` attribute and must be the only part in the attribute.")}render(t){return Object.keys(t).reduce((e,i)=>{const s=t[i];return s==null?e:e+`${i=i.includes("-")?i:i.replace(/(?:^(webkit|moz|ms|o)|)(?=[A-Z])/g,"-$&").toLowerCase()}:${s};`},"")}update(t,[e]){const{style:i}=t.element;if(this.ut===void 0){this.ut=new Set;for(const s in e)this.ut.add(s);return this.render(e)}this.ut.forEach(s=>{e[s]==null&&(this.ut.delete(s),s.includes("-")?i.removeProperty(s):i[s]="")});for(const s in e){const r=e[s];if(r!=null){this.ut.add(s);const n=typeof r=="string"&&r.endsWith(oi);s.includes("-")||n?i.setProperty(s,n?r.slice(0,-11):r,n?Et:""):i[s]=r}}return P}});/**
 * @license
 * Copyright 2018 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const ai=t=>t??y,li=t=>t instanceof Object&&typeof t.then=="function";class ye{constructor(){this.is_resolved=!1,this.is_rejected=!1,this.is_finished=!1,this.promise=new Promise((e,i)=>{this.resolve=s=>{try{li(s)?s.then(this.resolve,this.reject):(this.is_resolved=!0,this.is_finished=!0,e(this.value=s),this._runThen(),this._innerFinallyArg=Object.freeze({status:"resolved",result:this.value}),this._runFinally())}catch(r){this.reject(r)}},this.reject=s=>{this.is_rejected=!0,this.is_finished=!0,i(this.reason=s),this._runCatch(),this._innerFinallyArg=Object.freeze({status:"rejected",reason:this.reason}),this._runFinally()}})}static resolve(e){const i=new ye;return i.resolve(e),i}static sleep(e){const i=new ye;let s=setTimeout(()=>{s=void 0,i.resolve()},e);return i.onFinished(()=>s!==void 0&&clearTimeout(s)),i}onSuccess(e){this.is_resolved?this.__callInnerThen(e):(this._innerThen||(this._innerThen=[])).push(e)}onError(e){this.is_rejected?this.__callInnerCatch(e):(this._innerCatch||(this._innerCatch=[])).push(e)}onFinished(e){this.is_finished?this.__callInnerFinally(e):(this._innerFinally||(this._innerFinally=[])).push(e)}_runFinally(){if(this._innerFinally){for(const e of this._innerFinally)this.__callInnerFinally(e);this._innerFinally=void 0}}__callInnerFinally(e){queueMicrotask(async()=>{try{await e(this._innerFinallyArg)}catch(i){console.error("Unhandled promise rejection when running onFinished",e,i)}})}_runThen(){if(this._innerThen){for(const e of this._innerThen)this.__callInnerThen(e);this._innerThen=void 0}}_runCatch(){if(this._innerCatch){for(const e of this._innerCatch)this.__callInnerCatch(e);this._innerCatch=void 0}}__callInnerThen(e){queueMicrotask(async()=>{try{await e(this.value)}catch(i){console.error("Unhandled promise rejection when running onSuccess",e,i)}})}__callInnerCatch(e){queueMicrotask(async()=>{try{await e(this.value)}catch(i){console.error("Unhandled promise rejection when running onError",e,i)}})}}class Ge{constructor(e,i){this.id=e,this.src=i,this.webContentId=-1,this.webContentId_devTools=-1,this._api_po=new ye,this.closing=!1,this.state={zIndex:0,openingIndex:0,closingIndex:0,scale:1,opacity:1}}get api(){return this._api}doReady(e){this._api=e,this._api_po.resolve(e),console.log("执行了 doReady")}ready(){return this._api_po.promise}}const ci=`
(() => {
  console.log('--------')
  // if (!globalThis.__native_close_watcher_kit__) {
  //   globalThis.__native_close_watcher_kit__ =  {
  //     allc: 0,
  //     _watchers: new Map(),
  //     _tasks: new Map(),
  //     registryToken: function(consumeToken){
  //       if (consumeToken === null || consumeToken === "") {
  //         throw new Error("CloseWatcher.registryToken invalid arguments");
  //       }
  //       const resolve = this._tasks.get(consumeToken)
  //       if(resolve === undefined) throw new Error('resolve === undefined');
  //       const id = this.allc++;
  //       resolve(id + "");
  //     },
  //     tryClose: function(id){
  //       const watcher = this._watchers.get(id);
  //       if(watcher === undefined) throw new Error('watcher === undefined');
  //       watcher.dispatchEvent(new Event("close"))
  //     }
  //   };

  //   // 这里会修改了 window.open 的方法 是否有问题了？？
  //   globalThis.open = function(arg){
  //     console.error('open 方法被修改了 需要调用 主渲染进程的 openWebview 方法，但是还没有处理', arg)
  //   }
  // }

  // console.log('window: ', window)

  globalThis.fetch = () => {
    console.log(">>>>>>>>>>>>")
  } 
  
  // // 拦截 fetch
  // globalThis.nativeFetch = globalThis.fetch;
  // globalThis.fetch = (request) => {
  //   let url = typeof request === 'string' ? request : request.url;
  //   if(url.endsWith('bfs-metadata.json')){
  //     // 把请求发送出去
  //     console.log('需要拦截的请求', request)
  //     console.log('window.navigator.userAgent', window.navigator.userAgent);
  //     // 把请求发送给 jsMM 模块
  //     url = 'http://api.browser.sys.dweb-443.localhost:22605/open_download?url=' + url
  //     // 只能够想办法 发送给 browser 让 browser 处理
  //     return globalThis.nativeFetch(url)
  //   }else if(
  //     request.method == "GET" && request.url.host?.endsWith(".dweb")  && (request.url.scheme == "http" || request.url.scheme == "https")
  //   ){
  //     console.log('locstion', location)
  //   }else{
  //     return globalThis.nativeFetch(request)
  //   }
  // }

  // core 
  function inputBindVirtualKeyboard(el){
    el.removeEventListener('focusin',bindVirtualKeyboardFocusin)
    el.removeEventListener('focusout',bindVirtualKeyboardFocusout)
    el.addEventListener('focusin', bindVirtualKeyboardFocusin)
    el.addEventListener('focusout',bindVirtualKeyboardFocusout)
    console.log('bind virtual keyboard')
  }

  function bindVirtualKeyboardFocusin(){
    window.electron.ipcRenderer.sendToHost('virtual_keyboard_open')
  }

  function bindVirtualKeyboardFocusout(){
    window.electron.ipcRenderer.sendToHost('virtual_keyboard_close')
  }

  function inputIsNeedBindVirtualKeyboard(node){
    return node.tagName === "INPUT"
    && (
      node.type === "email"
      || node.type === "number"
      || node.type === "password"
      || node.type === "search"
      || node.type === "tel"
      || node.type === "text"
      || node.type === "url"
    ) 
  }

  function callback(mutationList, observe){
    mutationList.forEach(mutationRecord => {
      switch(mutationRecord.type){
        case "childList":
          mutationRecord.addedNodes.forEach(node => {
            // 添加了节点可能只有直接的子节点在这里，嵌套的子节点不再这里哦
            if(node.nodeType !== Node.ELEMENT_NODE) return;
            const allEl = getSub(node)
            allEl.forEach(el => {
              if(el.shadowRoot){
                // 添加 监听
                createMutationObserver(el.shadowRoot, callback)
                return;
              }
              // 绑定 virtual-keyboard
              inputIsNeedBindVirtualKeyboard(el)? inputBindVirtualKeyboard(el) : "";
            })
          })

          mutationRecord.removedNodes.forEach(node => {
            // 移除监听
            if(node.shadowRoot){
              node.shadowRoot.removeObserver();
            }
          })
        break;
      }
    })
  }

  function getSub(root){
    const sub = Array.from(root.children).reduce((pre, el) => {
      getSub(el)
      return [...pre, ...getSub(el)]
    },[])
    if(root.shadowRoot){
      return [...root.shadowRoot.children, ...sub]
    } 
    return [...root.children, ...sub]
  }

  function createMutationObserver(el, callback){
    const observerOptions = {
      childList: true,  // 观察目标子节点的变化，是否有添加或者删除
      subtree: true     // 观察后代节点，默认为 false
    }
    let observer = new MutationObserver(callback);
    observer.observe(el, observerOptions);
    console.error('observer 在什么时候会被回收')
    el.removeObserver = () => {
      console.log('回收了')
      observer = null
    }
  }

  createMutationObserver(document.body, callback)
  const allEl = getSub(document.body)
  allEl.forEach(el => {
    if(el.shadowRoot){
      createMutationObserver(el.shadowRoot, callback)
      console.log('添加了 observe')
      return;
    }
    inputIsNeedBindVirtualKeyboard(el)? inputBindVirtualKeyboard(el) : "";
  })
})()
`;var hi=Object.defineProperty,di=Object.getOwnPropertyDescriptor,C=(t,e,i,s)=>{for(var r=s>1?void 0:s?di(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&hi(e,i,r),r};let S=class extends _{constructor(){super(...arguments),this.customWebview=void 0,this.closing=!1,this.zIndex=0,this.scale=0,this.opacity=1,this.customWebviewId=0,this.src="",this.preload="",this.statusbarHidden=!1,this.executeJavascript=t=>{if(this.elWebview===void 0)throw new Error("this.elWebview === undefined");this.elWebview.executeJavaScript(t)}}onDomReady(t){this.dispatchEvent(new CustomEvent("dom-ready",{bubbles:!0,detail:{customWebview:this.customWebview,event:t,from:t.target}})),console.log("onDomReady")}webviewDidStartLoading(t){if(console.log("did-start-loading"),t.target===null)throw new Error("el === null");t.target.executeJavaScript(ci)}onAnimationend(t){this.dispatchEvent(new CustomEvent("animationend",{bubbles:!0,detail:{customWebview:this.customWebview,event:t,from:t.target}}))}onPluginNativeUiLoadBase(t){const e=t.target,i=e.contentWindow,s=e.parentElement;Reflect.set(i,"parentElement",s),i.postMessage("loaded","*")}getWebviewTag(){return this.elWebview}render(){const t=M({"--z-index":this.zIndex+"","--scale":this.scale+"","--opacity":this.opacity+""});return f`
      <webview
        nodeintegration
        nodeintegrationinsubframes
        allowpopups
        disablewebsecurity
        plugins
        class="webview ${this.closing?"closing-ani-view":"opening-ani-view"}"
        style="${t}"
        @animationend=${this.onAnimationend}
        data-app-url=${this.src}
        id="view-${this.customWebviewId}"
        class="webview"
        src=${ai(this.src)}
        partition="trusted"
        allownw
        preload=${this.preload}
        @dom-ready=${this.onDomReady}
        @did-start-loading=${this.webviewDidStartLoading}
        useragent=${navigator.userAgent+" dweb-host/"+location.host}
      ></webview>
    `}};S.styles=ui();C([d({type:Ge})],S.prototype,"customWebview",2);C([d({type:Boolean})],S.prototype,"closing",2);C([d({type:Number})],S.prototype,"zIndex",2);C([d({type:Number})],S.prototype,"scale",2);C([d({type:Number})],S.prototype,"opacity",2);C([d({type:Number})],S.prototype,"customWebviewId",2);C([d({type:String})],S.prototype,"src",2);C([d({type:String})],S.prototype,"preload",2);C([Ae()],S.prototype,"statusbarHidden",2);C([G("webview")],S.prototype,"elWebview",2);S=C([A("multi-webview-content")],S);function ui(){return[$`
      :host {
        box-sizing: border-box;
        margin: 0px;
        padding: 0px;
        width: 100%;
        height: 100%;
      }

      .webview {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        min-height: 100%;
        scrollbar-width: 2px;
        overflow: hidden;
        overflow-y: auto;
      }
    `,$`
      :host {
        --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
      }
      .opening-ani-view {
        animation: slideIn 520ms var(--easing) forwards;
      }
      .closing-ani-view {
        animation: slideOut 830ms var(--easing) forwards;
      }
      @keyframes slideIn {
        0% {
          transform: translateY(60%) translateZ(0);
          opacity: 0.4;
        }
        100% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
      }
      @keyframes slideOut {
        0% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
        30% {
          transform: translateY(-30%) translateZ(0) scale(0.4);
          opacity: 0.6;
        }
        100% {
          transform: translateY(-100%) translateZ(0) scale(0.3);
          opacity: 0.5;
        }
      }
    `]}var pi=Object.defineProperty,vi=Object.getOwnPropertyDescriptor,q=(t,e,i,s)=>{for(var r=s>1?void 0:s?vi(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&pi(e,i,r),r};const bi=[$`
    :host{
      width:100%;
      height:100%;
    }

    .container{
      width:100%;
      height:100%;
    }

    .toolbar{
      display: flex;
      justify-content: flex-start;
      align-items: flex-start;
      width:100%;
      height:60px;
    }

    .devtool{
      width:100%;
      height:calc(100% - 60px);
      border:1px solid #ddd;
    }
  `,$`
    :host {
      --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
    }
    .opening-ani-devtools {
      animation: slideIn 520ms var(--easing) forwards;
    }
    .closing-ani-devtools {
      animation: slideOut 830ms var(--easing) forwards;
    }
    @keyframes slideIn {
      0% {
        transform: translateY(60%) translateZ(0);
        opacity: 0.4;
      }
      100% {
        transform: translateY(0%) translateZ(0);
        opacity: 1;
      }
    }
    @keyframes slideOut {
      0% {
        transform: translateY(0%) translateZ(0);
        opacity: 1;
      }
      30% {
        transform: translateY(-30%) translateZ(0) scale(0.4);
        opacity: 0.6;
      }
      100% {
        transform: translateY(-100%) translateZ(0) scale(0.3);
        opacity: 0.5;
      }
    }
  `];let B=class extends _{constructor(){super(...arguments),this.customWebview=void 0,this.closing=!1,this.zIndex=0,this.scale=0,this.opacity=1,this.customWebviewId=0}onDomReady(t){this.dispatchEvent(new CustomEvent("dom-ready",{bubbles:!0,detail:{customWebview:this.customWebview,event:t,from:t.target}}))}onDestroy(){this.dispatchEvent(new Event("destroy-webview"))}render(){const t=M({"--z-index":this.zIndex+"","--scale":this.scale+"","--opacity":this.opacity+""});return f`
      <div 
        class="container ${this.closing?"closing-ani-devtools":"opening-ani-devtools"}" 
        style=${t}
      >
        <div class="toolbar">
            <button @click=${this.onDestroy}>销毁</button>
        </div>
        <webview
          id="tool-${this.customWebviewId}"
          class="devtool"
          src="about:blank"
          partition="trusted"
          @dom-ready=${this.onDomReady}
        ></webview>
      </div>
    `}};B.styles=bi;q([d({type:Ge})],B.prototype,"customWebview",2);q([d({type:Boolean})],B.prototype,"closing",2);q([d({type:Number})],B.prototype,"zIndex",2);q([d({type:Number})],B.prototype,"scale",2);q([d({type:Number})],B.prototype,"opacity",2);q([d({type:Number})],B.prototype,"customWebviewId",2);B=q([A("multi-webview-devtools")],B);/**
 * @license
 * Copyright 2021 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function ue(t,e,i){return t?e():i==null?void 0:i()}const H=typeof require<"u"?require("electron"):function(){return console.error('If you need to use "electron" in the Renderer process, make sure that "nodeIntegration" is enabled in the Main process.'),{}}();let pe;if(typeof document>"u"){pe={};const t=["invoke","postMessage","send","sendSync","sendTo","sendToHost","addListener","emit","eventNames","getMaxListeners","listenerCount","listeners","off","on","once","prependListener","prependOnceListener","rawListeners","removeAllListeners","removeListener","setMaxListeners"];for(const e of t)pe[e]=()=>{throw new Error(`ipcRenderer doesn't work in a Web Worker.
You can see https://github.com/electron-vite/vite-plugin-electron/issues/69`)}}else pe=H.ipcRenderer;H.clipboard;H.contextBridge;H.crashReporter;const O=pe;H.nativeImage;H.shell;H.webFrame;H.deprecate;function Ct(t){return{red:parseInt(t.slice(1,3),16),green:parseInt(t.slice(3,5),16),blue:parseInt(t.slice(5,7),16),alpha:parseInt(t.slice(7),16)}}var fi=Object.defineProperty,gi=Object.getOwnPropertyDescriptor,I=(t,e,i,s)=>{for(var r=s>1?void 0:s?gi(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&fi(e,i,r),r};let E=class extends _{constructor(){super(...arguments),this._color="#FFFFFFFF",this._style="DEFAULT",this._overlay=!1,this._visible=!0,this._height="38px",this._insets={top:this._height,right:0,bottom:0,left:0},this._torchIsOpen=!1,this._webview_src={}}createBackgroundStyleMap(){return{backgroundColor:this._visible?this._overlay?"transparent":this._color:"#000000FF"}}createContainerStyleMap(){const t=window.matchMedia("(prefers-color-scheme: light)");return{color:this._style==="LIGHT"?"#000000FF":this._style==="DARK"?"#FFFFFFFF":t?"#000000FF":"#FFFFFFFF"}}updated(t){const e=Array.from(t.keys());O.send("status_bar_state_change",new URL(this._webview_src).host.replace("www.","api."),{color:Ct(this._color),style:this._style,overlay:this._overlay,visible:this._visible,insets:this._insets}),(e.includes("_visible")||e.includes("_overlay"))&&this.dispatchEvent(new Event("safe_area_need_update"))}setHostStyle(){const t=this.renderRoot.host;t.style.position=this._overlay?"absolute":"relative",t.style.overflow=this._visible?"visible":"hidden"}render(){this.setHostStyle();const t=this.createBackgroundStyleMap(),e=this.createContainerStyleMap();return f`
      <div class="comp-container">
        <div class="background" style=${M(t)}></div>
        <div class="container" style=${M(e)}>
          ${ue(this._visible,()=>f`<div class="left_container">10:00</div>`,()=>f``)}
          <div class="center_container">
            ${ue(this._torchIsOpen,()=>f`<div class="torch_symbol"></div>`,()=>f``)}
          </div>
          ${ue(this._visible,()=>f`
                <div class="right_container">
                  <!-- 移动信号标志 -->
                  <svg 
                      t="1677291966287" 
                      class="icon icon-signal" 
                      viewBox="0 0 1024 1024" 
                      version="1.1" 
                      xmlns="http://www.w3.org/2000/svg" 
                      p-id="5745" 
                      width="32" 
                      height="32"
                  >
                      <path 
                          fill="currentColor"
                          d="M0 704h208v192H0zM272 512h208v384H272zM544 288h208v608H544zM816 128h208v768H816z" 
                          p-id="5746"
                      >
                      </path>
                  </svg>
      
                  <!-- wifi 信号标志 -->
                  <svg 
                      t="1677291873784" 
                      class="icon icon-wifi" 
                      viewBox="0 0 1024 1024" 
                      version="1.1" 
                      xmlns="http://www.w3.org/2000/svg" 
                      p-id="4699" 
                      width="32" 
                      height="32"
                  >
                      <path 
                          fill="currentColor"
                          d="M512 896 665.6 691.2C622.933333 659.2 569.6 640 512 640 454.4 640 401.066667 659.2 358.4 691.2L512 896M512 128C339.2 128 179.626667 185.173333 51.2 281.6L128 384C234.666667 303.786667 367.786667 256 512 256 656.213333 256 789.333333 303.786667 896 384L972.8 281.6C844.373333 185.173333 684.8 128 512 128M512 384C396.8 384 290.56 421.973333 204.8 486.4L281.6 588.8C345.6 540.586667 425.386667 512 512 512 598.613333 512 678.4 540.586667 742.4 588.8L819.2 486.4C733.44 421.973333 627.2 384 512 384Z" 
                          p-id="4700"
                      >
                      </path>
                  </svg>
      
                  <!-- 电池电量标志 -->
                  <svg 
                      t="1677291736404" 
                      class="icon icon-electricity" 
                      viewBox="0 0 1024 1024" 
                      version="1.1" 
                      xmlns="http://www.w3.org/2000/svg" 
                      p-id="2796" 
                      width="32" 
                      height="32"
                  >
                      <path 
                          fill="currentColor"
                          d="M984.2 434.8c-5-2.9-8.2-8.2-8.2-13.9v-99.3c0-53.6-43.9-97.5-97.5-97.5h-781C43.9 224 0 267.9 0 321.5v380.9C0 756.1 43.9 800 97.5 800h780.9c53.6 0 97.5-43.9 97.5-97.5v-99.3c0-5.8 3.2-11 8.2-13.9 23.8-13.9 39.8-39.7 39.8-69.2v-16c0.1-29.6-15.9-55.5-39.7-69.3zM912 702.5c0 12-6.2 19.9-9.9 23.6-3.7 3.7-11.7 9.9-23.6 9.9h-781c-11.9 0-19.9-6.2-23.6-9.9-3.7-3.7-9.9-11.7-9.9-23.6v-381c0-11.9 6.2-19.9 9.9-23.6 3.7-3.7 11.7-9.9 23.6-9.9h780.9c11.9 0 19.9 6.2 23.6 9.9 3.7 3.7 9.9 11.7 9.9 23.6v381z" 
                          fill="#606266" 
                          p-id="2797"
                      >
                      </path>
                      <path
                          fill="currentColor" 
                          d="M736 344v336c0 8.8-7.2 16-16 16H112c-8.8 0-16-7.2-16-16V344c0-8.8 7.2-16 16-16h608c8.8 0 16 7.2 16 16z" 
                          fill="#606266" 
                          p-id="2798"
                      >
                      </path>
                  </svg>
               </div>
              `,()=>f``)}
        </div>
      </div>
    `}};E.styles=yi();I([d({type:String})],E.prototype,"_color",2);I([d({type:String})],E.prototype,"_style",2);I([d({type:Boolean})],E.prototype,"_overlay",2);I([d({type:Boolean})],E.prototype,"_visible",2);I([d({type:String})],E.prototype,"_height",2);I([d({type:Object})],E.prototype,"_insets",2);I([d({type:Boolean})],E.prototype,"_torchIsOpen",2);I([d()],E.prototype,"_webview_src",2);E=I([A("multi-webview-comp-status-bar")],E);function yi(){return[$`
      :host{
        z-index: 999999999;
        flex-grow: 0;
        flex-shrink: 0;
        width: 100%;
        height: 38px;
      }

      .comp-container{
        --height: 48px;
        --cell-width: 80px;
        position: relative;
        width: 100%;
        height: 100%;
      }

      // html{
      //   width:100%;
      //   height: var(--height);
      //   overflow: hidden;
      // }

      .background{
        position: absolute;
        left: 0px;
        top: 0px;
        width: 100%;
        height: 100%;
        background: #FFFFFFFF;
      }

      .container{
        position: absolute;
        left: 0px;
        top: 0px;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height:100%;
      }

      .container-light{
        color: #FFFFFFFF;
      }

      .container-dark{
        color: #000000FF;
      }

      .container-default{
        color: #FFFFFFFF;
      }

      .left_container{
        display: flex;
        justify-content: center;
        align-items: flex-end;
        width: var(--cell-width);
        height: 100%;
        font-size: 15px;
        font-weight: 900;
      }

      .center_container{
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(100% - var(--cell-width) * 2);
        height: 100%;
        border-bottom-left-radius: var(--border-radius);
        border-bottom-right-radius: var(--border-radius);
      }

      .center_container::after{
        content: "";
        width: 50%;
        height: 20px;
        border-radius: 10px;
        background: #111111;
      }

      .torch_symbol{
        position: absolute;
        z-index: 1;
        width: 10px;
        height: 10px;
        border-radius: 20px;
        background: #fa541c;
      }

      .right_container{
        display: flex;
        justify-content: flex-start;
        align-items: flex-end;
        width: var(--cell-width);
        height: 100%;
      }

      .icon{
        margin-right: 5px;
        width: 18px;
        height: 18px;
      }
    `]}var wi=Object.defineProperty,_i=Object.getOwnPropertyDescriptor,Ft=(t,e,i,s)=>{for(var r=s>1?void 0:s?_i(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&wi(e,i,r),r};let we=class extends _{toastShow(t,e,i){var r;const s=document.createElement("multi-webview-comp-toast");[["_message",t],["_duration",e],["_position",i]].forEach(([n,o])=>{s.setAttribute(n,o)}),(r=this.appContentContainer)==null||r.append(s)}setHostStyle(){this.renderRoot.host}biometricsMock(){var e;const t=document.createElement("multi-webview-comp-biometrics");t.addEventListener("pass",()=>{this.dispatchEvent(new Event("biometrices-pass"))}),t.addEventListener("no-pass",()=>{this.dispatchEvent(new Event("biometrices-no-pass"))}),(e=this.appContentContainer)==null||e.appendChild(t),console.log("biometrics",t,this.appContentContainer)}hapticsMock(t){var i;console.log("hapticsMock",t);const e=document.createElement("multi-webview-comp-haptics");e.setAttribute("text",t),(i=this.appContentContainer)==null||i.appendChild(e)}shareShare(t){var i;const e=document.createElement("multi-webview-comp-share");[["_title",t.title],["_text",t.text],["_link",t.link],["_src",t.src]].forEach(([s,r])=>e.setAttribute(s,r)),(i=this.appContentContainer)==null||i.appendChild(e)}render(){return this.setHostStyle(),f`
      <div class="shell_container">
        <slot name="status-bar"></slot>
        <div class="app_content_container">
          <slot name="app_content">
            ... 桌面 ...
          </slot>
        </div>
        <slot name="bottom-bar"></slot>
      </div>
    `}};we.styles=mi();Ft([G(".app_content_container")],we.prototype,"appContentContainer",2);we=Ft([A("multi-webview-comp-mobile-shell")],we);function mi(){return[$`
      :host{
        overflow: hidden;
      }

      .shell_container{
        --width: 375px;
        position: relative;
        display: flex;
        flex-direction: column;
        box-sizing: content-box;
        width: var(--width);
        height: calc(var(--width) * 2.05);
        border: 10px solid #000000;
        border-radius: calc(var(--width) / 12);
        overflow: hidden;
      }

      .app_content_container{
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 100%;
      }
    `]}var $i=Object.defineProperty,Si=Object.getOwnPropertyDescriptor,J=(t,e,i,s)=>{for(var r=s>1?void 0:s?Si(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&$i(e,i,r),r};let T=class extends _{constructor(){super(...arguments),this._color="#ccccccFF",this._style="DEFAULT",this._overlay=!1,this._visible=!0,this._insets={top:0,right:0,bottom:20,left:0},this._webview_src={}}updated(t){const e=Array.from(t.keys());O.send("navigation_bar_state_change",new URL(this._webview_src).host.replace("www.","api."),{color:Ct(this._color),style:this._style,overlay:this._overlay,visible:this._visible,insets:this._insets}),(e.includes("_visible")||e.includes("_overlay"))&&this.dispatchEvent(new Event("safe_area_need_update"))}createBackgroundStyleMap(){return{backgroundColor:this._overlay?"transparent":this._color}}createContainerStyleMap(){const t=window.matchMedia("(prefers-color-scheme: light)");return{color:this._style==="LIGHT"?"#000000FF":this._style==="DARK"?"#FFFFFFFF":t?"#000000FF":"#FFFFFFFF"}}setHostStyle(){const t=this.renderRoot.host;t.style.position=this._overlay?"absolute":"relative",t.style.overflow=this._visible?"visible":"hidden"}back(){this.dispatchEvent(new Event("back"))}home(){console.error("navigation-bar click home 但是还没有处理")}menu(){console.error("navigation-bar 点击了menu 但是还没有处理")}render(){this.setHostStyle();const t=this.createBackgroundStyleMap(),e=this.createContainerStyleMap();return f`
      <div class="container">
        <div class="background" style=${M(t)}></div>
        <!-- android 导航栏 -->
        <div 
          class="navigation_bar_container"
          style=${M(e)}  
        >
          <div class="menu" @click="${this.menu}">
            <svg 
              class="icon_svg menu_svg"
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 448 512"
            >
              <path 
                fill="currentColor"
                d="M0 96C0 78.3 14.3 64 32 64H416c17.7 0 32 14.3 32 32s-14.3 32-32 32H32C14.3 128 0 113.7 0 96zM0 256c0-17.7 14.3-32 32-32H416c17.7 0 32 14.3 32 32s-14.3 32-32 32H32c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32H32c-17.7 0-32-14.3-32-32s14.3-32 32-32H416c17.7 0 32 14.3 32 32z"
              />
            </svg>
          </div>
          <div class="home" @click="${this.home}">
            <svg
              class="icon_svg" 
              xmlns="http://www.w3.org/2000/svg" 
              viewBox="0 0 512 512"
            >
              <path 
                fill="currentColor"
                d="M464 256A208 208 0 1 0 48 256a208 208 0 1 0 416 0zM0 256a256 256 0 1 1 512 0A256 256 0 1 1 0 256z"
              />
            </svg>
          </div>
          <div class="back" @click="${this.back}">
            <svg 
              class="icon_svg" 
              viewBox="0 0 1024 1024" 
              version="1.1" 
              xmlns="http://www.w3.org/2000/svg" 
            >
              <path 
                fill="currentColor"
                d="M814.40768 119.93088a46.08 46.08 0 0 0-45.13792 2.58048l-568.07424 368.64a40.42752 40.42752 0 0 0-18.75968 33.71008c0 13.39392 7.00416 25.96864 18.75968 33.66912l568.07424 368.64c13.35296 8.68352 30.72 9.66656 45.13792 2.58048a40.67328 40.67328 0 0 0 23.38816-36.2496v-737.28a40.71424 40.71424 0 0 0-23.38816-36.29056zM750.3872 815.3088L302.81728 524.86144l447.61088-290.44736v580.89472z" 
              >
              </path>
            </svg>
          </div>
        </div>
      </div>
    `}};T.styles=xi();J([d({type:String})],T.prototype,"_color",2);J([d({type:String})],T.prototype,"_style",2);J([d({type:Boolean})],T.prototype,"_overlay",2);J([d({type:Boolean})],T.prototype,"_visible",2);J([d({type:Object})],T.prototype,"_insets",2);J([d()],T.prototype,"_webview_src",2);T=J([A("multi-webview-comp-navigation-bar")],T);function xi(){return[$`
      :host{
        position: relative;
        z-index: 999999999;
        box-sizing: border-box;
        left: 0px;
        bottom: 0px;
        margin: 0px;
        width: 100%;
      }

      .container{
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 26px;
      }
      .background{
        position: absolute;
        top: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
        background: #FFFFFF00;
      }

      .line-container{
        position: absolute;
        top: 0px;
        left: 0px;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .line{
        width: 50%;
        height:4px;
        border-radius:4px;
      }

      .line-default{
        background: #FFFFFFFF;
      }

      .line-dark{
        background: #000000FF;
      }

      .line-light{
        background: #FFFFFFFF;
      }

      .navigation_bar_container{
        position: absolute;
        top: 0px;
        left: 0px;
        display: flex;
        justify-content: space-around;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .menu,
      .home,
      .back{
        display: flex;
        justify-content: center;
        align-items: center;
      }

      .icon_svg{
        width: 20px;
        height: 20px;
      }
    `]}/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const{I:Ai}=ii,lt=()=>document.createComment(""),ie=(t,e,i)=>{var s;const r=t._$AA.parentNode,n=e===void 0?t._$AB:e._$AA;if(i===void 0){const o=r.insertBefore(lt(),n),l=r.insertBefore(lt(),n);i=new Ai(o,l,t,t.options)}else{const o=i._$AB.nextSibling,l=i._$AM,c=l!==t;if(c){let a;(s=i._$AQ)===null||s===void 0||s.call(i,t),i._$AM=t,i._$AP!==void 0&&(a=t._$AU)!==l._$AU&&i._$AP(a)}if(o!==n||c){let a=i._$AA;for(;a!==o;){const v=a.nextSibling;r.insertBefore(a,n),a=v}}}return i},j=(t,e,i=t)=>(t._$AI(e,i),t),Ei={},Ci=(t,e=Ei)=>t._$AH=e,Fi=t=>t._$AH,Te=t=>{var e;(e=t._$AP)===null||e===void 0||e.call(t,!1,!0);let i=t._$AA;const s=t._$AB.nextSibling;for(;i!==s;){const r=i.nextSibling;i.remove(),i=r}};/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const ct=(t,e,i)=>{const s=new Map;for(let r=e;r<=i;r++)s.set(t[r],r);return s},R=De(class extends Ve{constructor(t){if(super(t),t.type!==Ke.CHILD)throw Error("repeat() can only be used in text expressions")}dt(t,e,i){let s;i===void 0?i=e:e!==void 0&&(s=e);const r=[],n=[];let o=0;for(const l of t)r[o]=s?s(l,o):o,n[o]=i(l,o),o++;return{values:n,keys:r}}render(t,e,i){return this.dt(t,e,i).values}update(t,[e,i,s]){var r;const n=Fi(t),{values:o,keys:l}=this.dt(e,i,s);if(!Array.isArray(n))return this.ht=l,o;const c=(r=this.ht)!==null&&r!==void 0?r:this.ht=[],a=[];let v,p,h=0,b=n.length-1,g=0,w=o.length-1;for(;h<=b&&g<=w;)if(n[h]===null)h++;else if(n[b]===null)b--;else if(c[h]===l[g])a[g]=j(n[h],o[g]),h++,g++;else if(c[b]===l[w])a[w]=j(n[b],o[w]),b--,w--;else if(c[h]===l[w])a[w]=j(n[h],o[w]),ie(t,a[w+1],n[h]),h++,w--;else if(c[b]===l[g])a[g]=j(n[b],o[g]),ie(t,n[h],n[b]),b--,g++;else if(v===void 0&&(v=ct(l,g,w),p=ct(c,h,b)),v.has(c[h]))if(v.has(c[b])){const F=p.get(l[g]),Ee=F!==void 0?n[F]:null;if(Ee===null){const Je=ie(t,n[h]);j(Je,o[g]),a[g]=Je}else a[g]=j(Ee,o[g]),ie(t,n[h],Ee),n[F]=null;g++}else Te(n[b]),b--;else Te(n[h]),h++;for(;g<=w;){const F=ie(t,a[w+1]);j(F,o[g]),a[g++]=F}for(;h<=b;){const F=n[h++];F!==null&&Te(F)}return this.ht=l,Ci(t,a),P}});/**
 * @license
 * Copyright 2018 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const Ot=De(class extends Ve{constructor(t){var e;if(super(t),t.type!==Ke.ATTRIBUTE||t.name!=="class"||((e=t.strings)===null||e===void 0?void 0:e.length)>2)throw Error("`classMap()` can only be used in the `class` attribute and must be the only part in the attribute.")}render(t){return" "+Object.keys(t).filter(e=>t[e]).join(" ")+" "}update(t,[e]){var i,s;if(this.it===void 0){this.it=new Set,t.strings!==void 0&&(this.nt=new Set(t.strings.join(" ").split(/\s/).filter(n=>n!=="")));for(const n in e)e[n]&&!(!((i=this.nt)===null||i===void 0)&&i.has(n))&&this.it.add(n);return this.render(e)}const r=t.element.classList;this.it.forEach(n=>{n in e||(r.remove(n),this.it.delete(n))});for(const n in e){const o=!!e[n];o===this.it.has(n)||!((s=this.nt)===null||s===void 0)&&s.has(n)||(o?(r.add(n),this.it.add(n)):(r.remove(n),this.it.delete(n)))}return P}});var Oi=Object.defineProperty,ki=Object.getOwnPropertyDescriptor,ae=(t,e,i,s)=>{for(var r=s>1?void 0:s?ki(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&Oi(e,i,r),r};let D=class extends _{constructor(){super(...arguments),this._visible=!1,this._overlay=!1,this._navigation_bar_height=0,this.timer=0,this.requestId=0,this.insets={left:0,top:0,right:0,bottom:0},this.maxHeight=0,this.row1Keys=["q","w","e","r","t","y","u","i","o","p"],this.row2Keys=["a","s","d","f","g","h","j","k","l"],this.row3Keys=["&#8679","z","x","c","v","b","n","m","&#10005"],this.row4Keys=["123","&#128512","space","search"]}setHostStyle(){const t=this.renderRoot.host;t.style.position=this._overlay?"absolute":"relative",t.style.overflow=this._visible?"visible":"hidden"}firstUpdated(){this.setCSSVar(),this.dispatchEvent(new Event("first-updated"))}setCSSVar(){if(!this._elContainer)throw new Error("this._elContainer === null");const e=this._elContainer.getBoundingClientRect().width/11,i=e*1,s=3,r=2;return this.maxHeight=(i+s*2)*4+i,[["--key-alphabet-width",e],["--key-alphabet-height",i],["--row-padding-vertical",s],["--row-padding-horizontal",r],["--height",this._navigation_bar_height]].forEach(([n,o])=>{var l;(l=this._elContainer)==null||l.style.setProperty(n,o+"px")}),this}repeatGetKey(t){return t}createElement(t,e){const i=document.createElement("div");return i.setAttribute("class",t),i.innerHTML=e,i}createElementForRow3(t,e,i){return this.createElement(i.startsWith("&")?t:e,i)}createElementForRow4(t,e,i,s){return this.createElement(s.startsWith("1")||s.startsWith("&")?t:s==="space"?e:i,s)}transitionstart(){this.timer=setInterval(()=>{this.dispatchEvent(new Event("height-changed"))},16)}transitionend(){this.dispatchEvent(new Event(this._visible?"show-completed":"hide-completed")),clearInterval(this.timer),this.dispatchEvent(new Event("height-changed"))}render(){this.setHostStyle();const t={container:!0,container_active:this._visible};return f`
      <div 
        class="${Ot(t)}"
        @transitionstart=${this.transitionstart}
        @transitionend=${this.transitionend}  
      >
          <div class="row line-1">
            ${R(this.row1Keys,this.repeatGetKey,this.createElement.bind(this,"key-alphabet"))}
          </div>   
          <div class="row line-2">
            ${R(this.row2Keys,this.repeatGetKey,this.createElement.bind(this,"key-alphabet"))}
          </div>  
          <div class="row line-3">
            ${R(this.row3Keys,this.repeatGetKey,this.createElementForRow3.bind(this,"key-symbol","key-alphabet"))}
          </div> 
          <div class="row line-4">
            ${R(this.row4Keys,this.repeatGetKey,this.createElementForRow4.bind(this,"key-symbol","key-space","key-search"))}
          </div> 
      </div>
    `}};D.styles=Mi();ae([G(".container")],D.prototype,"_elContainer",2);ae([d({type:Boolean})],D.prototype,"_visible",2);ae([d({type:Boolean})],D.prototype,"_overlay",2);ae([d({type:Number})],D.prototype,"_navigation_bar_height",2);D=ae([A("multi-webview-comp-virtual-keyboard")],D);function Mi(){return[$`
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
    `]}var Pi=Object.defineProperty,Bi=Object.getOwnPropertyDescriptor,le=(t,e,i,s)=>{for(var r=s>1?void 0:s?Bi(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&Pi(e,i,r),r};let W=class extends _{constructor(){super(...arguments),this._message="test message",this._duration="1000",this._position="top",this._beforeEntry=!0}firstUpdated(){setTimeout(()=>{this._beforeEntry=!1},0)}transitionend(t){if(this._beforeEntry){t.target.remove();return}setTimeout(()=>{this._beforeEntry=!0},parseInt(this._duration))}render(){const t={container:!0,before_entry:!!this._beforeEntry,after_entry:!this._beforeEntry,container_bottom:this._position==="bottom",container_top:this._position!=="bottom"};return f`
      <div 
        class=${Ot(t)}
        @transitionend=${this.transitionend}  
      >
        <p class="message">${this._message}</p>
      </div>
    `}};W.styles=Ti();W.properties={_beforeEntry:{state:!0}};le([d({type:String})],W.prototype,"_message",2);le([d({type:String})],W.prototype,"_duration",2);le([d({type:String})],W.prototype,"_position",2);le([Ae()],W.prototype,"_beforeEntry",2);W=le([A("multi-webview-comp-toast")],W);function Ti(){return[$`
      .container{
        position: absolute;
        left: 0px;
        box-sizing: border-box;
        padding: 0px 20px;
        width: 100%;
        transition: all 0.25s ease-in-out;
      }

      .container_bottom{
        bottom: 0px;
      }

      .container_top{
        top: 0px;
      }

      .before_entry{
        transform: translateX(-100vw);
      }

      .after_entry{
        transform: translateX(0vw)
      }

      .message{
        box-sizing: border-box;
        padding: 0px 6px;
        width: 100%;
        height: 38px;
        color: #FFFFFF;
        line-height: 38px;
        text-align: left;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        background: #eee;
        border-radius: 5px;
        background: #1677ff;
      }
    `]}/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const Ii=Symbol.for(""),Ri=t=>{if((t==null?void 0:t.r)===Ii)return t==null?void 0:t._$litStatic$},ht=new Map,Wi=t=>(e,...i)=>{const s=i.length;let r,n;const o=[],l=[];let c,a=0,v=!1;for(;a<s;){for(c=e[a];a<s&&(n=i[a],(r=Ri(n))!==void 0);)c+=r+e[++a],v=!0;a!==s&&l.push(n),o.push(c),a++}if(a===s&&o.push(e[s]),v){const p=o.join("$$lit$$");(e=ht.get(p))===void 0&&(o.raw=o,ht.set(p,e=o)),i=l}return t(e,...i)},kt=Wi(f);var Hi=Object.defineProperty,Li=Object.getOwnPropertyDescriptor,Mt=(t,e,i,s)=>{for(var r=s>1?void 0:s?Li(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&Hi(e,i,r),r};let _e=class extends _{firstUpdated(t){if(this.elInput===void 0||this.elInput===null)throw new Error("this.elInput === undefined || this.elInput === null");this.elInput.click()}onChange(t){t.target,console.log("onChange")}render(){return kt`
      <input type="file" @change=${this.onChange}/>
    `}};_e.styles=ji();Mt([G("input")],_e.prototype,"elInput",2);_e=Mt([A("multi-webview-comp-barcode-scanning")],_e);function ji(){return[$`
      position: fixed;
      left: 0px;
      top: 0px;
      width: 0px;
      height: 0px;
      overflow: hidden;
    `]}var zi=Object.defineProperty,Ni=Object.getOwnPropertyDescriptor,Ui=(t,e,i,s)=>{for(var r=s>1?void 0:s?Ni(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&zi(e,i,r),r};let He=class extends _{pass(){var t;console.error("点击了 pass 但是还没有处理"),this.dispatchEvent(new Event("pass")),(t=this.shadowRoot)==null||t.host.remove()}noPass(){var t;console.error("点击了 no pass 但是还没有处理"),this.dispatchEvent(new Event("no-pass")),(t=this.shadowRoot)==null||t.host.remove()}render(){return kt`
      <div class="panel">
        <p>点击按钮 模拟 返回结果</p>
        <div class="btn_group">
          <button class="pass" @click=${this.pass}>识别通过</button>
          <button class="no_pass" @click=${this.noPass}>识别没通过</button>
        </div>
      </div>
    `}};He.styles=Ki();He=Ui([A("multi-webview-comp-biometrics")],He);function Ki(){return[$`
      :host{
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 100px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #00000033;
      }

      .panel{
        padding: 12px 20px;
        width: 80%;
        border-radius: 12px;
        background: #FFFFFFFF;
      }

      .btn_group{
        width: 100%;
        display: flex;
        justify-content: space-between;
      }

      .pass,
      .no_pass{
        padding: 8px 20px;
        border-radius: 5px;
        border: none;

      }

      .pass{
        color: #FFFFFFFF;
        background: #1677ff;    
      }

      .no_pass{
        background: #d9d9d9;
      }
    `]}var Di=Object.defineProperty,Vi=Object.getOwnPropertyDescriptor,Pt=(t,e,i,s)=>{for(var r=s>1?void 0:s?Vi(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&Di(e,i,r),r};let me=class extends _{constructor(){super(...arguments),this.text=""}firstUpdated(){var t;(t=this.shadowRoot)==null||t.host.addEventListener("click",this.cancel)}cancel(){var t;(t=this.shadowRoot)==null||t.host.remove()}render(){return f`
      <div class="panel">
        <p>模拟: ${this.text}</p>
        <div class="btn_group">
          <button class="btn" @click=${this.cancel}>取消</button>
        </div>
      </div>
    `}};me.styles=Gi();Pt([d({type:String})],me.prototype,"text",2);me=Pt([A("multi-webview-comp-haptics")],me);function Gi(){return[$`
      :host{
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 100px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #00000033;
        cursor: pointer;
      }

      .panel{
        padding: 12px 20px;
        width: 80%;
        border-radius: 12px;
        background: #FFFFFFFF;
      }

      .btn_group{
        width: 100%;
        display: flex;
        justify-content: flex-end;
      }
      
      .btn{
        padding: 8px 20px;
        border-radius: 5px;
        border: none;
        color: #FFFFFFFF;
        background: #1677ff; 

      }
    `]}var qi=Object.defineProperty,Ji=Object.getOwnPropertyDescriptor,ce=(t,e,i,s)=>{for(var r=s>1?void 0:s?Ji(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&qi(e,i,r),r};let V=class extends _{constructor(){super(...arguments),this._title="标题 这里是超长的标题，这里是超长的标题这里是超长的，这里是超长的标题，这里是超长的标题",this._text="文本内容 这里是超长的内容，这里是超长的内容，这里是超长的内容，这里是超长的内容，",this._link="http://www.baidu.com?url=",this._src="https://img.tukuppt.com/photo-big/00/00/94/6152bc0ce6e5d805.jpg"}firstUpdated(t){var e;(e=this.shadowRoot)==null||e.host.addEventListener("click",this.cancel)}cancel(){var t;(t=this.shadowRoot)==null||t.host.remove()}render(){return f`
      <div class="panel">
        <img class="img" src=${this._src}></img>
        <div class="text_container">
          <h2 class="h2">${this._title}</h2>
          <p class="p">${this._text}</p>
          <a class="a" href=${this._link} target="_blank">${this._link}</a>
        </div>
      </div>
    `}};V.styles=Zi();ce([d({type:String})],V.prototype,"_title",2);ce([d({type:String})],V.prototype,"_text",2);ce([d({type:String})],V.prototype,"_link",2);ce([d({type:String})],V.prototype,"_src",2);V=ce([A("multi-webview-comp-share")],V);function Zi(){return[$`
      :host{
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 200px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #000000cc;
        cursor: pointer;
        backdrop-filter: blur(5px);
      }

      .panel{
        display: flex;
        flex-direction: column;
        justify-content: center;
        width: 70%;
        border-radius: 6px;
        background: #FFFFFFFF;
        border-radius: 6px;
        overflow: hidden;
      }

      .img{
        display: block;
        box-sizing: border-box;
        padding: 30px;
        max-width: 100%;
        max-height: 300px;
      }

      .text_container{
        box-sizing: border-box;
        padding: 20px;
        width: 100%;
        height: auto;
        background: #000000FF;
      }

      .h2{
        margin: 0px;
        padding: 0px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 16px;
        color: #fff;
      }

      .p{
        margin: 0px;
        padding: 0px;
        font-size: 13px;
        color: #666;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .a{
        display: block;
        font-size: 12px;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    `]}const Yi=`
  (() => {
    console.log("1")
    const watchers = Array.from(globalThis.__native_close_watcher_kit__._watchers.values())
    console.log("2", watchers, watchers.length, history.state)
    if(watchers.length === 0){
      if(history.state === null || history.state.back === null){
        window.electron.ipcRenderer.sendToHost(
          'webveiw_message',
          "back"
        )
        console.log('window.electron:',window.electron)
      }else{
        window.history.back();
      }
    }else{
      watchers[watchers.length - 1].close()
    }
    console.log("3")
  })()
`;/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */const Bt=Symbol("Comlink.proxy"),Xi=Symbol("Comlink.endpoint"),Qi=Symbol("Comlink.releaseProxy"),Ie=Symbol("Comlink.finalizer"),ve=Symbol("Comlink.thrown"),Tt=t=>typeof t=="object"&&t!==null||typeof t=="function",es={canHandle:t=>Tt(t)&&t[Bt],serialize(t){const{port1:e,port2:i}=new MessageChannel;return qe(t,e),[i,[i]]},deserialize(t){return t.start(),Wt(t)}},ts={canHandle:t=>Tt(t)&&ve in t,serialize({value:t}){let e;return t instanceof Error?e={isError:!0,value:{message:t.message,name:t.name,stack:t.stack}}:e={isError:!1,value:t},[e,[]]},deserialize(t){throw t.isError?Object.assign(new Error(t.value.message),t.value):t.value}},It=new Map([["proxy",es],["throw",ts]]);function is(t,e){for(const i of t)if(e===i||i==="*"||i instanceof RegExp&&i.test(e))return!0;return!1}function qe(t,e=globalThis,i=["*"]){e.addEventListener("message",function s(r){if(!r||!r.data)return;if(!is(i,r.origin)){console.warn(`Invalid origin '${r.origin}' for comlink proxy`);return}const{id:n,type:o,path:l}=Object.assign({path:[]},r.data),c=(r.data.argumentList||[]).map(z);let a;try{const v=l.slice(0,-1).reduce((h,b)=>h[b],t),p=l.reduce((h,b)=>h[b],t);switch(o){case"GET":a=p;break;case"SET":v[l.slice(-1)[0]]=z(r.data.value),a=!0;break;case"APPLY":a=p.apply(v,c);break;case"CONSTRUCT":{const h=new p(...c);a=je(h)}break;case"ENDPOINT":{const{port1:h,port2:b}=new MessageChannel;qe(t,b),a=as(h,[h])}break;case"RELEASE":a=void 0;break;default:return}}catch(v){a={value:v,[ve]:0}}Promise.resolve(a).catch(v=>({value:v,[ve]:0})).then(v=>{const[p,h]=xe(v);e.postMessage(Object.assign(Object.assign({},p),{id:n}),h),o==="RELEASE"&&(e.removeEventListener("message",s),Rt(e),Ie in t&&typeof t[Ie]=="function"&&t[Ie]())}).catch(v=>{const[p,h]=xe({value:new TypeError("Unserializable return value"),[ve]:0});e.postMessage(Object.assign(Object.assign({},p),{id:n}),h)})}),e.start&&e.start()}function ss(t){return t.constructor.name==="MessagePort"}function Rt(t){ss(t)&&t.close()}function Wt(t,e){return Le(t,[],e)}function he(t){if(t)throw new Error("Proxy has been released and is not useable")}function Ht(t){return X(t,{type:"RELEASE"}).then(()=>{Rt(t)})}const $e=new WeakMap,Se="FinalizationRegistry"in globalThis&&new FinalizationRegistry(t=>{const e=($e.get(t)||0)-1;$e.set(t,e),e===0&&Ht(t)});function rs(t,e){const i=($e.get(e)||0)+1;$e.set(e,i),Se&&Se.register(t,e,t)}function ns(t){Se&&Se.unregister(t)}function Le(t,e=[],i=function(){}){let s=!1;const r=new Proxy(i,{get(n,o){if(he(s),o===Qi)return()=>{ns(r),Ht(t),s=!0};if(o==="then"){if(e.length===0)return{then:()=>r};const l=X(t,{type:"GET",path:e.map(c=>c.toString())}).then(z);return l.then.bind(l)}return Le(t,[...e,o])},set(n,o,l){he(s);const[c,a]=xe(l);return X(t,{type:"SET",path:[...e,o].map(v=>v.toString()),value:c},a).then(z)},apply(n,o,l){he(s);const c=e[e.length-1];if(c===Xi)return X(t,{type:"ENDPOINT"}).then(z);if(c==="bind")return Le(t,e.slice(0,-1));const[a,v]=dt(l);return X(t,{type:"APPLY",path:e.map(p=>p.toString()),argumentList:a},v).then(z)},construct(n,o){he(s);const[l,c]=dt(o);return X(t,{type:"CONSTRUCT",path:e.map(a=>a.toString()),argumentList:l},c).then(z)}});return rs(r,t),r}function os(t){return Array.prototype.concat.apply([],t)}function dt(t){const e=t.map(xe);return[e.map(i=>i[0]),os(e.map(i=>i[1]))]}const Lt=new WeakMap;function as(t,e){return Lt.set(t,e),t}function je(t){return Object.assign(t,{[Bt]:!0})}function xe(t){for(const[e,i]of It)if(i.canHandle(t)){const[s,r]=i.serialize(t);return[{type:"HANDLER",name:e,value:s},r]}return[{type:"RAW",value:t},Lt.get(t)||[]]}function z(t){switch(t.type){case"HANDLER":return It.get(t.name).deserialize(t.value);case"RAW":return t.value}}function X(t,e,i){return new Promise(s=>{const r=ls();t.addEventListener("message",function n(o){!o.data||!o.data.id||o.data.id!==r||(t.removeEventListener("message",n),s(o.data))}),t.start&&t.start(),t.postMessage(Object.assign({id:r},e),i)})}function ls(){return new Array(4).fill(0).map(()=>Math.floor(Math.random()*Number.MAX_SAFE_INTEGER).toString(16)).join("-")}const jt=t=>(ut(t,"addEventListener",1),ut(t,"removeEventListener",1),cs(t),t),ut=(t,e,i)=>{const s=t[e];return t[e]=function(...r){return r[i]=zt(r[i]),s.apply(this,r)},t},Re=new WeakMap,zt=t=>{if(typeof t=="object")return t.handleEvent=zt(t.handleEvent),t;let e=Re.get(t);return e===void 0&&(e=function(i){return JSON.stringify(i.data,function(r,n){return Array.isArray(n)&&n[0]==="#PORT#"?(this[r]=i.ports[n[1]],null):n}),t.call(this,i)},Re.set(t,e),Re.set(e,t)),e},cs=t=>{const e=t.postMessage;return t.postMessage=function(i,s){Array.isArray(s)?(JSON.stringify(i,function(n,o){if(o&&typeof o=="object"&&"postMessage"in o){const l=s.indexOf(o);if(l!==-1)return this[n]=["#PORT#",l],null}return o}),e.call(this,i,s)):s?e.call(this,i,s):e.call(this,i)},t},hs=t=>{let e=!1,i;return(...s)=>(e===!1&&(e=!0,i=t(...s)),i)},Nt=new MessageChannel,Ut=new MessageChannel,Kt=Nt.port1,Dt=Ut.port1;O.postMessage("renderPort",{},[Nt.port2,Ut.port2]);jt(Kt);jt(Dt);const Vt=Kt;let Gt={};const qt=()=>{qe(Gt,Vt)};Object.assign(globalThis,{mainPort:Vt,start:qt});const ds=hs(t=>{Gt=t,qt()}),Z=Wt(Dt);function us(t,e,i){return{visible:e?i.visible:t.visible,overlay:e?i.overlay:t.overlay,insets:e?i.insets:t.insets}}var ps=Object.defineProperty,vs=Object.getOwnPropertyDescriptor,x=(t,e,i,s)=>{for(var r=s>1?void 0:s?vs(e,i):e,n=t.length-1,o;n>=0;n--)(o=t[n])&&(r=(s?o(e,i,r):o(r))||r);return s&&r&&ps(e,i,r),r};let m=class extends _{constructor(){super(...arguments),this._id_acc=0,this.webviews=[],this.statusBarHeight="38px",this.navigationBarHeight="26px",this.virtualKeyboardAnimationSetIntervalId=0,this.name="Multi Webview",this.statusBarState=[],this.navigationBarState=[],this.safeAreaState=[],this.isShowVirtualKeyboard=!1,this.virtualKeyboardState={insets:{top:0,right:0,bottom:0,left:0},overlay:!1,visible:!1},this.torchState={isOpen:!1},this.preloadAbsolutePath="",this.safeAreaGetState=()=>{const t=this.navigationBarState[0],e=this.statusBarState[0],i=us(t,this.isShowVirtualKeyboard,this.virtualKeyboardState);return{overlay:this.safeAreaState[0].overlay,insets:{left:0,top:e.visible?e.overlay?e.insets.top:0:e.insets.top,right:0,bottom:i.visible&&i.overlay?i.insets.bottom:0},cutoutInsets:{left:0,top:e.insets.top,right:0,bottom:0},outerInsets:{left:0,top:e.visible?e.overlay?0:e.insets.top:0,right:0,bottom:i.visible?i.overlay?0:i.insets.bottom:0}}},this.safeAreaSetOverlay=t=>{const e=this.safeAreaState;return e[0].overlay=t,this.safeAreaState=e,this.barSetState("statusBarState","overlay",t),this.barSetState("navigationBarState","overlay",t),this.virtualKeyboardSetOverlay(t),this.safeAreaGetState()},this.safeAreaNeedUpdate=()=>{O.send("safe_area_update",new URL(this.webviews[this.webviews.length-1].src).host.replace("www.","api."),this.safeAreaGetState())},this.navigationBarOnBack=()=>{var i;this.webviews.length;const t=this.webviews[0],e=new URL(t.src).origin;(i=this._multiWebviewContent)==null||i.forEach(s=>{if(s.src.includes(e)){const r=s.getWebviewTag();r==null||r.addEventListener("ipc-message",this.webviewTagOnIpcMessageHandlerBack)}}),this.executeJavascriptByHost(e,Yi)},this.webviewTagOnIpcMessageHandlerBack=t=>{const e=Reflect.get(t,"channel"),i=Reflect.get(t,"args");e==="webveiw_message"&&i[0]==="back"&&t.target!==null&&(t.target.removeEventListener("ipc-message",this.webviewTagOnIpcMessageHandlerBack),this.navigationBarState=this.navigationBarState.slice(1),this.statusBarState=this.statusBarState.slice(1),O.send("safe_are_insets_change"),O.send("navigation_bar_state_change"),O.send("status_bar_state_change")),this.webviews.length===1?Z.closedBrowserWindow():this.destroyWebview(this.webviews[0])},this.webviewTagOnIpcMessageHandlerNormal=t=>{const e=Reflect.get(t,"channel");switch(Reflect.get(t,"args"),e){case"virtual_keyboard_open":this.isShowVirtualKeyboard=!0;break;case"virtual_keyboard_close":this.virtualKeyboardState={...this.virtualKeyboardState,visible:!1};break;case"webveiw_message":this.webviewTagOnIpcMessageHandlerBack(t);break;default:throw new Error(`webview ipc-message 还有没有处理的channel===${e}`)}}}barSetState(t,e,i){const s=this[t];return s.length,s[0][e]=i,t==="navigationBarState"&&e==="visible"&&(s[0].insets.bottom=i?parseInt(this.navigationBarHeight):0),this[t]=JSON.parse(JSON.stringify(s)),this[t][0]}barGetState(t){const e=this[t].length-1;return this[t][e]}virtualKeyboardStateUpdateInsetsByEl(){var i;const t=(i=this.multiWebviewCompVirtualKeyboard)==null?void 0:i.getBoundingClientRect().height;if(t===void 0)throw new Error("height === undefined");const e=this.navigationBarState[this.navigationBarState.length-1].insets.bottom;this.virtualKeyboardState={...this.virtualKeyboardState,insets:{...this.virtualKeyboardState.insets,bottom:t<=e?0:t}},this.safeAreaNeedUpdate()}virtualKeyboardFirstUpdated(){this.virtualKeyboardState={...this.virtualKeyboardState,visible:!0}}virtualKeyboardHideCompleted(){this.isShowVirtualKeyboard=!1,O.send("safe_are_insets_change"),clearInterval(this.virtualKeyboardAnimationSetIntervalId)}virtualKeyboardShowCompleted(){O.send("safe_are_insets_change"),clearInterval(this.virtualKeyboardAnimationSetIntervalId)}virtualKeyboardSetOverlay(t){const e={...this.virtualKeyboardState,overlay:t};return this.virtualKeyboardState=e,e}virtualKeyboardGetState(){return this.virtualKeyboardState}toastShow(t,e,i){var s;(s=this.multiWebviewCompMobileShell)==null||s.toastShow(t,e,i)}torchStateToggle(){const t={...this.torchState,isOpen:!this.torchState.isOpen};return this.torchState=t,t.isOpen}torchStateGet(){return this.torchState.isOpen}barcodeScanningGetPhoto(){const t=document.createElement("multi-webview-comp-barcode-scanning");document.body.append(t)}biometricsMock(){var t;return(t=this.multiWebviewCompMobileShell)==null||t.biometricsMock(),new Promise(e=>this.biometricesResolve=e)}biometricessPass(t){var e;(e=this.biometricesResolve)==null||e.call(this,t)}hapticsSet(t){var e;return(e=this.multiWebviewCompMobileShell)==null||e.hapticsMock(t),!0}shareShare(t){var e;(e=this.multiWebviewCompMobileShell)==null||e.shareShare(t)}_restateWebviews(){let t=0,e=0,i=0,s=.05,r=1+s,n=.1,o=1+n;for(const l of this.webviews)l.state.zIndex=this.webviews.length-++t,l.closing?l.state.closingIndex=e++:(l.state.scale=r-=s,s=Math.max(0,s-.01),l.state.opacity=o-n,o=Math.max(0,o-n),l.state.openingIndex=i++);this.requestUpdate("webviews")}openWebview(t){const e=this._id_acc++;return this.webviews.unshift(new Ge(e,t)),this._restateWebviews(),this.webviews.length===1?(this.statusBarState=[pt("statusbar",this.statusBarHeight)],this.navigationBarState=[pt("navigationbar",this.navigationBarHeight)],this.safeAreaState=[fs()]):(this.webviews.length,this.statusBarState=[{...this.statusBarState[0],insets:{...this.statusBarState[0].insets}},...this.statusBarState],this.navigationBarState=[{...this.navigationBarState[0],insets:{...this.navigationBarState[0].insets}},...this.navigationBarState],this.safeAreaState=[{...this.safeAreaState[0]},...this.safeAreaState]),e}closeWebview(t){const e=this.webviews.find(i=>i.id===t);return e===void 0?!1:(e.closing=!0,this._restateWebviews(),!0)}closeWindow(){Z.closedBrowserWindow()}_removeWebview(t){const e=this.webviews.indexOf(t);return e===-1?!1:(this.webviews.splice(e,1),this._restateWebviews(),!0)}async onWebviewReady(t,e){t.webContentId=e.getWebContentsId(),t.doReady(e),Z.denyWindowOpenHandler(t.webContentId,je(i=>{this.openWebview(i.url)})),Z.onDestroy(t.webContentId,je(()=>{this.closeWebview(t.id),console.log("Destroy!!")})),e==null||e.addEventListener("ipc-message",this.webviewTagOnIpcMessageHandlerNormal)}async onDevtoolReady(t,e){await t.ready(),t.webContentId_devTools!==e.getWebContentsId()&&(t.webContentId_devTools=e.getWebContentsId(),await Z.openDevTools(t.webContentId,void 0,t.webContentId_devTools))}async deleteTopBarState(){this.statusBarState=this.statusBarState.slice(1),this.navigationBarState=this.navigationBarState.slice(1)}async deleteTopSafeAreaState(){this.safeAreaState=this.safeAreaState.slice(1)}async destroyWebview(t){console.log("destroyWebview: "),await Z.destroy(t.webContentId),this.deleteTopBarState(),this.deleteTopSafeAreaState()}async destroyWebviewByHost(t){return this.webviews.forEach(e=>{new URL(e.src).host===t&&this.destroyWebview(e)}),!0}async destroyWebviewByOrigin(t){return console.log("destroyWebviewByOrigin: "),this.webviews.forEach(e=>{e.src.includes(t)&&this.destroyWebview(e)}),!0}async restartWebviewByHost(t){return this._restateWebviews(),!0}async executeJavascriptByHost(t,e){var i;(i=this._multiWebviewContent)==null||i.forEach(s=>{const r=new URL(s.src.split("?")[0]).origin,n=new URL(t).origin;if(s.src.includes(t)||r===n){s.executeJavascript(e);return}})}async acceptMessageFromWebview(t){switch(t.action){case"history_back":this.destroyWebviewByOrigin(t.origin);break;default:console.error("acceptMessageFromWebview 还有没有处理的 action = "+t.action)}}preloadAbsolutePathSet(t){this.preloadAbsolutePath=t}render(){const t=this.statusBarState[0],e=this.navigationBarState[0];return this.webviews,f`
      <div class="app-container">
        <multi-webview-comp-mobile-shell
          @biometrices-pass=${()=>this.biometricessPass(!0)}
          @biometrices-no-pass=${()=>this.biometricessPass(!1)}
        >
          ${R(this.webviews,i=>i.src,(i,s)=>s===0?f`
                    <multi-webview-comp-status-bar 
                      slot="status-bar" 
                      ._color=${t.color}
                      ._style = ${t.style}
                      ._overlay = ${t.overlay}
                      ._visible = ${t.visible}
                      ._height = ${this.statusBarHeight}
                      ._inserts = ${t.insets}
                      ._torchIsOpen=${this.torchState.isOpen}
                      ._webview_src=${i.src}
                      @safe_area_need_update=${this.safeAreaNeedUpdate}
                    ></multi-webview-comp-status-bar>
                  `:f``)}
          ${R(this.webviews,i=>i.id,i=>{const s=M({zIndex:i.state.zIndex+""});return f`
                <multi-webview-content
                  slot="app_content"
                  .customWebview=${i}
                  .closing=${i.closing}
                  .zIndex=${i.state.zIndex}
                  .scale=${i.state.scale}
                  .opacity=${i.state.opacity}
                  .customWebviewId=${i.id}
                  .src=${i.src}
                  .preload=${this.preloadAbsolutePath}
                  style=${s}
                  @animationend=${r=>{r.detail.event.animationName==="slideOut"&&r.detail.customWebview.closing&&this._removeWebview(i)}} 
                  @dom-ready=${r=>{this.onWebviewReady(i,r.detail.event.target)}}
                  data-app-url=${i.src}
                ></multi-webview-content>
              `})}
          ${R(this.webviews,i=>i.src,(i,s)=>s===0?f`
                    ${ue(this.isShowVirtualKeyboard,()=>f`
                        <multi-webview-comp-virtual-keyboard
                          slot="bottom-bar"
                          ._navigation_bar_height=${this.navigationBarState[this.navigationBarState.length-1].insets.bottom}
                          ._visible=${this.virtualKeyboardState.visible}
                          ._overlay=${this.virtualKeyboardState.overlay}
                          ._webview_src=${i.src}
                          @first-updated=${this.virtualKeyboardFirstUpdated}
                          @hide-completed=${this.virtualKeyboardHideCompleted} 
                          @show-completed=${this.virtualKeyboardShowCompleted}
                          @height-changed=${this.virtualKeyboardStateUpdateInsetsByEl}
                        ></multi-webview-comp-virtual-keyboard>
                      `,()=>{const r=M({"flex-grow":"0","flex-sharink":"0",height:e.visible?this.navigationBarHeight:"0px"});return f`
                          <multi-webview-comp-navigation-bar
                            style=${r}
                            slot="bottom-bar"
                            ._color=${e.color}
                            ._style = ${e.style}
                            ._overlay = ${e.overlay}
                            ._visible = ${e.visible}
                            ._inserts = ${e.insets}
                            ._webview_src=${i.src}
                            @back=${this.navigationBarOnBack}
                            @safe_area_need_update=${this.safeAreaNeedUpdate}
                          ></multi-webview-comp-navigation-bar>
                        `})}
                  `:f``)}
          
        </multi-webview-comp-mobile-shell>
      </div>
      <div class="dev-tools-container">
        ${R(this.webviews,i=>i.id,i=>{const s=M({zIndex:i.state.zIndex+""});return f`
                <multi-webview-devtools
                  .customWebview=${i}
                  .closing=${i.closing}
                  .zIndex=${i.state.zIndex}
                  .scale=${i.state.scale}
                  .opacity=${i.state.opacity}
                  .customWebviewId=${i.id}
                  style="${s}"
                  @dom-ready=${r=>{this.onDevtoolReady(i,r.detail.event.target)}}
                  @destroy-webview=${()=>this.destroyWebview(i)}
                ></multi-webview-devtools>
              `})}
      </div>
    `}};m.styles=bs();x([ni("multi-webview-content")],m.prototype,"_multiWebviewContent",2);x([G("multi-webview-comp-mobile-shell")],m.prototype,"multiWebviewCompMobileShell",2);x([G("multi-webview-comp-virtual-keyboard")],m.prototype,"multiWebviewCompVirtualKeyboard",2);x([G("multi-webview-comp-navigation-bar")],m.prototype,"multiWebviewCompNavigationBar",2);x([d()],m.prototype,"name",2);x([d({type:Object})],m.prototype,"statusBarState",2);x([d({type:Object})],m.prototype,"navigationBarState",2);x([d({type:Object})],m.prototype,"safeAreaState",2);x([d({type:Boolean})],m.prototype,"isShowVirtualKeyboard",2);x([d({type:Object})],m.prototype,"virtualKeyboardState",2);x([Ae()],m.prototype,"torchState",2);x([Ae()],m.prototype,"preloadAbsolutePath",2);m=x([A("view-tree")],m);const u=new m;document.body.appendChild(u);const Jt={openWebview:u.openWebview.bind(u),closeWebview:u.closeWebview.bind(u),closeWindow:u.closeWindow.bind(u),destroyWebviewByHost:u.destroyWebviewByHost.bind(u),restartWebviewByHost:u.restartWebviewByHost.bind(u),executeJavascriptByHost:u.executeJavascriptByHost.bind(u),acceptMessageFromWebview:u.acceptMessageFromWebview.bind(u),statusBarSetState:u.barSetState.bind(u,"statusBarState"),statusBarGetState:u.barGetState.bind(u,"statusBarState"),navigationBarSetState:u.barSetState.bind(u,"navigationBarState"),navigationBarGetState:u.barGetState.bind(u,"navigationBarState"),safeAreaSetOverlay:u.safeAreaSetOverlay.bind(u),safeAreaGetState:u.safeAreaGetState.bind(u),virtualKeyboardGetState:u.virtualKeyboardGetState.bind(u),virtualKeyboardSetOverlay:u.virtualKeyboardSetOverlay.bind(u),toastShow:u.toastShow.bind(u),shareShare:u.shareShare.bind(u),torchStateToggle:u.torchStateToggle.bind(u),torchStateGet:u.torchStateGet.bind(u),hapticsSet:u.hapticsSet.bind(u),biometricsMock:u.biometricsMock.bind(u),preloadAbsolutePathSet:u.preloadAbsolutePathSet.bind(u)};ds(Jt);function bs(){return[$`
      :host {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        height: 100%;
        background: #00000022;
      }

      .app-container{
        flex-grow: 0;
        flex-shrink: 0;
      }

      .dev-tools-container{
        flex-grow: 100;
        flex-shrink: 100;
        min-width:500px;
        height: 100%;
      }
    `]}function pt(t,e){return{color:"#FFFFFFFF",style:"DEFAULT",insets:{top:t==="statusbar"?parseInt(e):0,right:0,bottom:t==="navigationbar"?parseInt(e):0,left:0},overlay:!1,visible:!0}}function fs(){return{overlay:!1}}Object.assign(globalThis,Jt);
