import { BasePlugin } from '../basePlugin.ts';
import { Duration, Position, IShowOptions } from "./toast.type.ts";
import { IToastPlugin } from './toast.type.ts';
/**
 * 访问 toast 能力的插件
 */

export class ToastPlugin extends BasePlugin implements IToastPlugin {
    // override shadowRoot: ShadowRoot | null; 
    private _root: ShadowRoot;
    // private _elContent: HTMLDivElement = document.createElement('div');
    private _elStyle: HTMLStyleElement = document.createElement('style');
    private _fragment: DocumentFragment = new DocumentFragment()
    private _duration: Duration = "long"
    private _position: Position = "bottom"
    // deno-lint-ignore no-inferrable-types
    private _verticalClassName: string = ""

    constructor(readonly mmid = "toast.sys.dweb") {
        super(mmid, "Toast")
        this._root = this.attachShadow({ mode: 'open' });
        this._init()
    }

    private _init() {
        // this._initContent()._initStyle()._initfragment()._initShadowRoot()
    }

    // private _initfragment() {
    //     this._fragment.append(this._elContent, this._elStyle);
    //     return this;
    // }

    // private _initShadowRoot() {
    //     this._root.appendChild(this._fragment)
    //     return this;
    // }

    // private _initContent() {
    //     this._elContent.setAttribute("class", "content")
    //     this._elContent.innerText = '消息的内容！';
    //     return this;
    // }

    // private _initStyle() {
    //     this._elStyle.setAttribute("type", "text/css")
    //     this._elStyle.innerText = createCssText()
    //     return this;
    // }

    /**
     * toast信息显示
     * @param message 消息
     * @param duration 时长 'long' | 'short'
     * @returns
     */
    async show(options: IShowOptions) {
        const { text, duration = "long", position = "bottom" } = options;
        this._duration = duration;
        this._position = position;
        this.setAttribute('style', "left: 0px;")
        // this._elContent.innerText = text
        // this._elContentTransitionStart()
        return await this.nativeFetch(`/show?message=${text}&duration=${duration}&position=${position}`)
    }

    connectedCallback() {

    }

    // private _onTransitionenOutToIn = () => {
    //     setTimeout(() => {
    //         this._elContent.removeEventListener('transitionend', this._onTransitionenOutToIn)
    //         this._elContent.addEventListener('transitionend', this._onTransitionendInToOut)
    //         this._elContent.classList.remove("content_transform_inside")
    //         this._elContent.classList.add("content_transform_outside")

    //     }, this._duration === "long" ? 2000 : 3500)
    // }

    // private _onTransitionendInToOut = () => {
    //     this._elContent.removeEventListener('transitionend', this._onTransitionendInToOut)
    //     this._elContent.classList.remove("content_transform_outside")
    //     this._elContent.classList.remove("content_transition")
    //     this._elContent.classList.remove(this._verticalClassName)
    //     this.setAttribute('style', "")
    // }

    // private _elContentTransitionStart = () => {
    //     this._verticalClassName = this._position === "bottom" ? "content_vertical_bottom" : this._position === "center" ? "content_vertical_center" : "content_vertical_top"
    //     this._elContent.classList.add("content_transform_outside", this._verticalClassName)
    //     this._elContent.addEventListener('transitionend', this._onTransitionenOutToIn)
    //     setTimeout(() => {
    //         this._elContent.classList.remove("content_transform_outside")
    //         this._elContent.classList.add("content_transform_inside", "content_transition")
    //     }, 100)
    // }

}



// function createCssText() {
//     return `
//         :host{
//             position: fixed;
//             z-index: 9999999999;
//             left: -100vw;
//             top: 0;
//             display: flex;
//             justify-content: center;
//             align-items: center;
//             box-sizing: border-box;
//             width: 100%;
//             height: 100%;
//             border: 1px solid red;
//         }

//         .content{
//             position: absolute;
//             box-sizing: border-box;
//             padding: 0px 10px;
//             min-width: 10px;
//             max-width: 300;
//             font-size: 13px;
//             line-height: 30px;
//             overflow: hidden;
//             whilte-space: nowrap;
//             text-overflow: ellipsis;
//             border-radius: 10px;
//             color: #fff;
//             background: #000d;
//         }
        
//         .content_vertical_top{
//             top: 10px;
//         }

//         .content_vertical_center{

//         }

//         .content_vertical_bottom{
//             bottom: 30px;
//         }

//         .content_transform_outside{
//             transform: translateX(100vw);
//         }

//         .content_transform_inside{
//             transform: translateX(0);
//         }

//         .content_transition{
//             transition: all 0.5s ease-out;
//         }
    
//     `
// }

