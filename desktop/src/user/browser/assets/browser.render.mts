import {css, LitElement, html } from "lit";
import { property, state, query } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";

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
            width: var(--size);
            height: var(--size);
            border-radius: 16px;
            background-color: #ddd;
        }

        .item-container:nth-of-type(2n){
            margin: 0px calc((100% - var(--size) * 3) / 2);
        }
         
    `
]
class HomePage extends LitElement{
    @property() apps: any[] = []

    @query(".search-input") elInput: HTMLInputElement | undefined;

    static override styles = styles
    protected override render(){
        const arr: any[][] = toTwoDimensionalArray(this.apps)
        return html`
            <div 
                class="page-container"
            >
                <div class="logo-container">logo</div>
                <div class="search-container">
                   <input class="search-input" placeholder="search app"/>
                   <button class="search-bottom" @click=${this.onSearch}>search</button>
                </div>
                <div class="apps-container">
                    ${
                        repeat(arr, (rows, index) => index, (rows,index) => (
                            html `
                                <div class="row-container">
                                    ${
                                        repeat(rows, item => item.id, item => (
                                            html `
                                                <div class="item-container">${item.id}</div>
                                            `
                                        ))
                                    }
                                </div>
                            `
                        ))
                    }
                </div>
            </div>
        `
    }

    override connectedCallback(){
        super.connectedCallback();
        this.apps =  [0,1,2,3,4,5,6,7,8,9,10].map(id => ({id: id}))
    }

    onSearch(){
        console.log('搜索的value： ', this.elInput?.value)
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
 

 