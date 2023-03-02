import {css, LitElement, html } from "lit";
import { styleMap } from "lit/directives/style-map.js";
import { property, state, query } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { customElement } from "lit/decorators.js";
import type { $AppInfo } from "../../../sys/file/file-get-all-apps.cjs"
const styles = [
    css `
        .page-container{
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            align-items: center;
            box-sizing: border-box;
            height:100%;
            
        }

        .logo-container{
            display: flex;
            justify-content: center;
            align-items:flex-start;
            margin-top: 30px;
            width: 100px;
            height: 60px;
            background: #0001;
        }

        .search-container{
            display: flex;
            justify-content: center;
            margin-top: 66px;
            width: 80%;
            height: 48px;
            border-radius: 50px;
            background: #0001;
            overflow: hidden;
            border: 1px solid #ddd;
        }

        .search-input{
            box-sizing: border-box;
            padding: 0px 16px;
            flex-grow: 1;
            width: 10px;
            height: 100%;
            outline: none;
            border: none;
        }

        .search-input::placeholder {
            color: #ddd;
            text-align: center;
          }

        .search-bottom{
            flex: 0 0 88px;
            height: 48px;
            line-height: 48px;
            text-align: center;
            color: #666;
            border: none;
        }

        .apps-container{
            width: 80%;
            height: auto;
        }

        .row-container{
            --size: 60px;
            display: flex;
            justify-content: flex-start;
            padding-top: 30px;
            height: var(--size);
        }

        .item-container{
            display: flex;
            justify-content: center;
            align-items: center;
            flex-grow: 0;
            flex-shrink: 0;
            box-sizing: border-box;
            padding:10px;
            width: var(--size);
            height: var(--size);
            border-radius: 16px;
            background-color: #ddd1;
            background-position: center;
            background-size: contain;
            background-repeat: no-repeat;
            cursor: pointer;
        }

        .item-container:nth-of-type(2n){
            margin: 0px calc((100% - var(--size) * 3) / 2);
        }
         
    `
]
@customElement('home-page')
class HomePage extends LitElement{
    @property() apps:  $AppInfo[] = []

    @query(".search-input") elInput: HTMLInputElement | undefined;

    static override styles = styles

    constructor(){
        super()
        this.getAllAppsInfo()
    }

    protected override render(){
        const arr:  $AppInfo[][] = toTwoDimensionalArray(this.apps)
        return html`
            <div 
                class="page-container"
            >
                <div class="logo-container">logo---</div>
                <div class="search-container">
                   <input class="search-input" placeholder="search app" value="https://shop.plaoc.com/W85DEFE5/W85DEFE5.bfsa"/>
                   <button class="search-bottom" @click=${this.onView} >view</button>
                </div>
                <div class="apps-container">
                    ${
                        repeat(arr, (rows, index) => index, (rows,index) => (
                            html `
                                <div class="row-container">
                                    ${
                                        repeat(rows, item => item.bfsAppId, item => {
                                            return  html `
                                                <app-col .item=${item} class="item-container" @click=${() => this.onOpenApp(item.appId)}></app-col>
                                            `
                                        })
                                    }
                                </div>
                            `
                        ))
                    }
                </div>

                <!-- 实验该改变状态栏 -->
                <!-- <button @click=${() => this.setStatusbarBackground("#F00F")}>设置状态栏的颜色 === #F00F</button>
                <button @click=${() => this.setStatusbarBackground("#0F0F")}>设置状态栏的颜色 === #0F0F</button>
                <button @click=${() => this.setStatusbarBackground("#00FF")}>设置状态栏的颜色 === #00FF</button>
                <button @click=${() => this.setStatusbarStyle("light")}>设置状态栏的风格 === light</button>onSearch
                <button @click=${() => this.setStatusbarStyle("dark")}>设置状态栏的风格 === dark</button>
                <button @click=${() => this.setStatusbarStyle("default")}>设置状态栏的风格 === default</button>
                <button @click=${() => this.getStatusbarStyle()}>获取状态栏的风格</button>
                <button @click=${() => this.setStatusbarOverlays("0")}>获取状态栏的overlays 不覆盖</button>
                <button @click=${() => this.setStatusbarOverlays("1")}>获取状态栏的overlays 覆盖</button> -->
                <!-- <button @click=${() => { open(`/index.html?qaq=${encodeURIComponent(Date.now())}`)}}>open</button> -->
               
            </div>
        `
    }

    override connectedCallback(){
        super.connectedCallback();
    }

    // 查看信息
    onView = () => {
        fetch("/open_webview?mmid=jmmmetadata.sys.dweb")
        .then(async (res) => {
            console.log('res', res)
            const result = JSON.parse(await res.json())
            const origin = result.origin;
            open(origin)
            
        })
        .catch(err => console.log('err')) 
    }

    // // 下载的功能
    // onSearch(){
    //     fetch(`./download?url=${this.elInput?.value}`)
    //     .then(async (res) => {
    //         console.log('下载成功了---res: ', await res.json())
          
    //     })
    //     .then(this.getAllAppsInfo)
    //     .catch(err => console.log('下载失败了'))
    // }

    getAllAppsInfo(){
        console.log('开始获取 全部 appsInfo')
        fetch(`./appsinfo`)
        .then(async (res) => {
            console.log('res: ', res)
            const _json = await res.json()
            this.apps = JSON.parse(_json)

        })
        .catch(err => {
            console.log('获取全部 appsInfo error: ', err)
        })
    }

    async onOpenApp(appId: string){
        let response = await fetch(`./install?appId=${appId}`)
        if(response.status !== 200){ // 安装成功
            console.error('安装应用失败 appId: ', appId, response.text())
            return ;
        }  
        // 打开一个新的 window 窗口
        response = await fetch(`./open?appId=${appId}`)
    }
     
    
    async setStatusbarBackground(color: string){
        // 测试是否可以通过直接向 statusbar.sys.dweb 发送消息实现了？？
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.setBackgroundColor(color)
    }

    async setStatusbarStyle(value: string){
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.setStyle(value)
    }
 
    async getStatusbarStyle(){
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.getStyle()
    }

    async setStatusbarOverlays(value: string){
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.setOverlaysWebview(value)
    }
    
}

/**
 * 把一维数组转化为二位数组
 * @param items 
 * @returns 
 */
function toTwoDimensionalArray(items: unknown[]){
    let twoDimensionalArr: any[][] = []
    items.forEach((item, index) => {
        const rowIndex = Math.floor(index / 3)
        const colIndex = index % 3
        twoDimensionalArr[rowIndex] = twoDimensionalArr[rowIndex] ? twoDimensionalArr[rowIndex] : [];
        twoDimensionalArr[rowIndex][colIndex] = item
    })
    return twoDimensionalArr
}

@customElement('app-col')
class AppCol extends LitElement{
    @property() item: $AppInfo | undefined = undefined
    static override styles = [
        css`
            .container{
                display: flex;
                justify-content: center;
                align-items: center;
                box-sizing: border-box;
                padding:10px;
                width: 100%;
                height: 100%;
                border-radius: 16px;
                background-color: #ddd1;
                background-position: center;
                background-size: contain;
                background-repeat: no-repeat;
                cursor: pointer;
            }
        `
    ]

    protected override render() {
        const _styleMap = styleMap({
            backgroundImage: "url(./icon/"+ this.item?.bfsAppId +"/sys"+ this.item?.icon +")"
        })
        return html`<div class="container" style=${_styleMap} ></div>`
    }
}
 