## account

```md
- portal
  - bfs-all-accounts.json
- index.html
- bfsa-metadata.json
```

```ts
openBrl;

const accountApp = () => {
    openBrl('account:add-account')
  for await (
    accounts of subPortal<{title:string,url:string}>("portal+json:bfs-addable-account-types?schema=")
  ) {
    // const accounts = await fetchPortal("portal+json:bfs-addable-account-types");
    html`
    <select id="$account_type">
        ${
      accounts.map((account) => {
        return html`
            <option ${account.url}>${account.title}</option>
        `;
      })
    }
    </select>
    <img src="data:svg+xml;">
    <area></area>
    <button @click=${() => { openPortal($account_type.value); }}>添加账户</button>
    `;
  }
};
const accountRequestPermission = ()=>{
    
}
```

生物校验
给一个面板，包含要签名的数据信息，Next
_最好是不需要主密码_
安全密码确定，Next
等待上链面板，Complete

## bfm

````md
- portal
  - bfs-addable-account-types.json.js
    ```ts
    export const on_portal_request  = (req, res) => { // 150ms
        if (isMatch(req)) {
            res.offer();
            res.end([{title:"添加BFM账户",url:"${APP_ID}/bfm-add-account.html"}]) 
        } 
    }
    ```
  - bfs-addable-account-types.json
- bfm-add-account.html
- index.html
- bfsa-metadata.json

````
给一个面板，包含要签名的数据信息，Next
主密码确定，Next
安全密码确定，Next
等待上链面板，Complete

## social-app

```ts
const social = await fetchPortal(
  "portal+svg:bfs-all-accounts?type=social&theme=red",
);
```
