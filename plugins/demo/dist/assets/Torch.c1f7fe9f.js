import{_ as h}from"./toast.7a5f524e.js";import{t as b,_ as p,d as g}from"./LogPanel.4a208a19.js";import{d as k,r as _,o as T,c as v,a as u,b as t,u as s,F as y,g as P,f as C}from"./index.37913e46.js";const $={class:"card glass"},w={class:"card-body"},B=t("h2",{class:"card-title"},"Torch",-1),S={class:"justify-end card-actions"},x=t("div",{class:"divider"},"LOG",-1),j=k({__name:"Torch",setup(F){const f="Toast",o=_(),c=_();let l,n;T(()=>{l=b(o),n=c.value});const r=g(async()=>n.toggleTorch(),{name:"toggleTorch",args:[],logPanel:o}),i=g(async()=>{const d=await n.getTorchState();l.info("torch state",d)},{name:"getState",args:[],logPanel:o});return(d,e)=>{const m=C("dweb-torch");return P(),v(y,null,[u(m,{ref_key:"$torchPlugin",ref:c},null,512),t("div",$,[t("figure",{class:"icon"},[t("img",{src:h,alt:f})]),t("article",w,[B,t("div",S,[t("button",{class:"inline-block rounded-full btn btn-accent",onClick:e[0]||(e[0]=(...a)=>s(r)&&s(r)(...a))},"toggle"),t("button",{class:"inline-block rounded-full btn btn-accent",onClick:e[1]||(e[1]=(...a)=>s(i)&&s(i)(...a))},"state")])])]),x,u(p,{ref_key:"$logPanel",ref:o},null,512)],64)}}});export{j as default};
