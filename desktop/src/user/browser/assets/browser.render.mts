import {css, LitElement, html } from "lit";
import { styleMap } from "lit/directives/style-map.js";
import { property, state, query } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
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
        console.log("location: ", location)
        console.log('window: ', window)
        
        return html`
            <div 
                class="page-container"
            >
                <div class="logo-container">logo---</div>
                <div class="search-container">
                   <input class="search-input" placeholder="search app" value="https://shop.plaoc.com/W85DEFE5/W85DEFE5.bfsa"/>
                   <button class="search-bottom" @click=${this.onSearch}>DOWNLOAD</button>
                </div>
                <div class="apps-container">
                    ${
                        repeat(arr, (rows, index) => index, (rows,index) => (
                            html `
                                <div class="row-container">
                                    ${
                                        repeat(rows, item => item.bfsAppId, item => {
                                            const _styleMap = styleMap({
                                                backgroundImage: "url(./icon/"+ item.bfsAppId +"/sys"+ item.icon +")"
                                            })
                                            return  html `
                                                <div 
                                                    class="item-container"
                                                    style="${_styleMap}"
                                                    @click=${() => this.onOpenApp(item.appId)}
                                                >
                                                </div>
                                            `
                                        })
                                    }
                                </div>
                            `
                        ))
                    }
                </div>

                <!-- 实验该改变状态栏 -->
                <button @click=${() => this.setStatusbarBackground("#F00F")}>设置状态栏的颜色 === #F00F</button>
                <button @click=${() => this.setStatusbarBackground("#0F0F")}>设置状态栏的颜色 === #0F0F</button>
                <button @click=${() => this.setStatusbarBackground("#00FF")}>设置状态栏的颜色 === #00FF</button>
                <button @click=${() => this.setStatusbarStyle("light")}>设置状态栏的风格 === light</button>
                <button @click=${() => this.setStatusbarStyle("dark")}>设置状态栏的风格 === dark</button>
                <button @click=${() => this.setStatusbarStyle("default")}>设置状态栏的风格 === default</button>
                <button @click=${() => this.getStatusbarStyle()}>获取状态栏的风格</button>
                <button @click=${() => this.setStatusbarOverlays("0")}>获取状态栏的overlays 不覆盖</button>
                <button @click=${() => this.setStatusbarOverlays("1")}>获取状态栏的overlays 覆盖</button>
             

            </div>
        `
    }

    // .style=${{
    //     backgroundImage: "url(/icon?id="+ item.bfsAppId +"&name="+ item.icon +")",
    //     backgroundColor: "red"
    // }} 
    //  style=${styleMap({backgroundImage: "url(./icon/"+ item.bfsAppId +"/sys"+ item.icon +")"})}

    override connectedCallback(){
        super.connectedCallback();
        var bc = new BroadcastChannel('aaa');
    }

    onSearch(){
        fetch(`./download?url=${this.elInput?.value}`)
        .then(async (res) => {
            console.log('下载成功了---res: ', await res.json())
          
        })
        .then(this.getAllAppsInfo)
        .catch(err => console.log('下载失败了'))
    }

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
        // console.log('开始打开应用:open:', open)
        // 需要 open appId 指定的应用
        // 通过 open 打开的 应用 location.host 应该是当前 域名下的  path 不同而已
        // http://browser.sys.dweb-80.localhost:22605/ 还是会发送给 brower.worker.ts
        // http://browser.sys.dweb-80.localhost:22605/index.html?qaq=1677033788241 可以理解为这一个应用下的多个子应用 附着应用？？？
        // open(`/index.html?qaq=${encodeURIComponent(Date.now())}`);
         
        // 打开一个新的 window 窗口
        response = await fetch(`./open?appId=${appId}`)
    }
 
    async setStatusbarBackground(color: string){
        // 测试是否可以通过直接向 statusbar.sys.dweb 发送消息实现了？？
        console.log('dweb.render.mts 点击二零设置背景色')
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.setBackgroundColor(color)
        console.log('dweb.render.mts 设置了背景色： ', result)
    }

    async setStatusbarStyle(value: string){
        console.log('dweb.render.mts 点击二零设置 样式')
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.setStyle(value)
        console.log('dweb.render.mts 设置了样式返回： ', result)
    }

    account = 0;
    async getStatusbarStyle(){
        console.log('this.account: ', this.account)
        console.log('dweb.render.mts 点击获取样式返回： ')
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        // const result = await el.getStyle()
        // console.log('dweb.render.mts 获取样式返回： ', await result.json())
        el.getStyle().then(async (res) => console.log(await res.json()))

        
        // 测试快速获取 样式
        if(this.account >= 100)return;
        this.account++;
        this.getStatusbarStyle()
    }

    async setStatusbarOverlays(value: string){
        console.log('点击了 设置 overlays',value)
        const el = document.querySelector('statusbar-dweb') 
        if(el === null) return console.error('设置 statusbar错误 el === null')
        // @ts-ignore
        const result = await el.setOverlaysWebview(value)
        console.log('result: ', await result)
    }
}
 
customElements.define('home-page', HomePage)

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



 

 