/**
 * 访问 toast 能力的插件
 * 
 1"
 */
class ToastPlugin extends HTMLElement{
    // override shadowRoot: ShadowRoot | null; 
    private _root: ShadowRoot; 
    private _elContent:HTMLDivElement = document.createElement('div');
    private _elStyle: HTMLStyleElement = document.createElement('style');
    private _fragment: DocumentFragment = new DocumentFragment()
    private _duration: $Duration = "long"
    private _position: $Position = "bottom"
    private _verticalClassName: string = ""

    constructor(){
        super()
        this._root = this.attachShadow({mode:'open'});
        this._init()
    }

    private _init(){
        this._initContent()._initStyle()._initfragment()._initShadowRoot()
    }
    
    private _initfragment(){
        this._fragment.append(this._elContent, this._elStyle);
        return this;
    }

    private _initShadowRoot(){
        this._root.appendChild(this._fragment)
        return this;
    }

    private _initContent(){
        this._elContent.setAttribute("class", "content")
        this._elContent.innerText = '消息的内容！';
        return this;
    }

    private _initStyle(){
        this._elStyle.setAttribute("type", "text/css")
        this._elStyle.innerText = createCssText()
        return this;
    }

    show(message: string, duration:$Duration = "long", position: $Position ="bottom"){
        this._duration = duration;
        this._position = position;
        this.setAttribute('style', "left: 0px;")
        this._elContent.innerText = message
        this._elContentTransitionStart()
        return this;
    }

    connectedCallback(){
        // 问题是否可以同时显示多条
        // 如果是连续的显示该如何操作
        // console.log('载入了', this)
        // const arr = ["bottom", "top", "center"]
        // let index = 0;
        // setInterval(() =>{
        //     this.show("message,messagemessagemessagemessagemessagemessagemessage", "long", arr[index++] as any);
        //     index = index > 2 ? 0 : index;
        // }, 6000)

        
        // setInterval(() =>{
        //     this.show("message", "short", arr[index++] as any);
        //     index = index > 2 ? 0 : index;
        // }, 6000)
        
    }

    private _onTransitionenOutToIn = () => {
        setTimeout(() => {
            this._elContent.removeEventListener('transitionend', this._onTransitionenOutToIn)
            this._elContent.addEventListener('transitionend', this._onTransitionendInToOut)
            this._elContent.classList.remove("content_transform_inside")
            this._elContent.classList.add("content_transform_outside")
            
        }, this._duration === "long" ? 2000 : 3500)
    }

    private _onTransitionendInToOut = () => {
        this._elContent.removeEventListener('transitionend', this._onTransitionendInToOut)
        this._elContent.classList.remove("content_transform_outside")
        this._elContent.classList.remove("content_transition")
        this._elContent.classList.remove(this._verticalClassName)
        this.setAttribute('style', "")
    }

    private _elContentTransitionStart = () => {
        this._verticalClassName = this._position === "bottom" ? "content_vertical_bottom" : this._position === "center" ? "content_vertical_center" : "content_vertical_top"
        this._elContent.classList.add("content_transform_outside", this._verticalClassName)
        this._elContent.addEventListener('transitionend', this._onTransitionenOutToIn)
        setTimeout(() => {
            this._elContent.classList.remove("content_transform_outside")
            this._elContent.classList.add("content_transform_inside", "content_transition")
        }, 100)
    }

};

customElements.define('toast-dweb', ToastPlugin)
 
// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded(){
    const el = new ToastPlugin();
    document.body.append(el);
    document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
};

function createCssText(){
    return `
        :host{
            position: fixed;
            z-index: 9999999999;
            left: -100vw;
            top: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            box-sizing: border-box;
            width: 100%;
            height: 100%;
            border: 1px solid red;
        }

        .content{
            position: absolute;
            box-sizing: border-box;
            padding: 0px 10px;
            min-width: 10px;
            max-width: 300;
            font-size: 13px;
            line-height: 30px;
            overflow: hidden;
            whilte-space: nowrap;
            text-overflow: ellipsis;
            border-radius: 10px;
            color: #fff;
            background: #000d;
        }
        
        .content_vertical_top{
            top: 10px;
        }

        .content_vertical_center{

        }

        .content_vertical_bottom{
            bottom: 30px;
        }

        .content_transform_outside{
            transform: translateX(100vw);
        }

        .content_transform_inside{
            transform: translateX(0);
        }

        .content_transition{
            transition: all 0.5s ease-out;
        }
    
    `
}

export type $Duration =  'long' | 'short';
export type $Position = 'top' | 'center' | 'bottom'