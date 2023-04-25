import{d as O,r as v,o as x,c as G,a as b,b as t,u as a,w as g,v as D,e as P,F as L,f as U,g as $}from"./index.37913e46.js";import{t as j,_ as A,d as c}from"./LogPanel.4a208a19.js";import{V as F}from"./VColorPicker.749e7ce9.js";var M="/assets/navigationbar.fceda033.svg";const E={class:"card glass"},T={class:"card-body"},H=t("h2",{class:"card-title"},"Navigation Bar Background Color",-1),I={class:"justify-end card-actions btn-group"},K=["disabled"],R={class:"card-body"},W=t("h2",{class:"card-title"},"Navigation Bar Style",-1),q=t("option",{value:"DEFAULT"},"Default",-1),z=t("option",{value:"DARK"},"Dark",-1),J=t("option",{value:"LIGHT"},"Light",-1),Q=[q,z,J],X={class:"justify-end card-actions btn-group"},Y=["disabled"],Z={class:"card-body"},tt=t("h2",{class:"card-title"},"Navigation Bar Overlays WebView",-1),et={class:"justify-end card-actions btn-group"},lt=["disabled"],at={class:"card-body"},nt=t("h2",{class:"card-title"},"Navigation Bar Visible",-1),ot={class:"justify-end card-actions btn-group"},st=["disabled"],it=t("div",{class:"divider"},"LOG",-1),vt=O({__name:"NavigationBar",setup(dt){const h="NavigationBar",n=v(),y=v();let _,o;x(async()=>{_=j(n),o=y.value,m(await o.getState(),"init")});const m=(u,e)=>{s.value=u.color,i.value=u.style,d.value=u.overlay,r.value=u.visible,_.log(e,u)},s=v(null),f=c(async()=>{await o.setColor(s.value)},{name:"setColor",args:[s],logPanel:n}),p=c(async()=>{s.value=await o.getColor()},{name:"getColor",args:[s],logPanel:n}),i=v(null),k=c(async()=>{await o.setStyle(i.value)},{name:"setStyle",args:[i],logPanel:n}),C=c(async()=>{i.value=await o.getStyle()},{name:"getStyle",args:[i],logPanel:n}),d=v(null),V=c(()=>o.setOverlay(d.value),{name:"setOverlay",args:[d],logPanel:n}),w=c(async()=>{d.value=await o.getOverlay()},{name:"getOverlay",args:[d],logPanel:n}),r=v(null),B=c(()=>o.setVisible(r.value),{name:"setVisible",args:[r],logPanel:n}),S=c(async()=>{r.value=await o.getVisible()},{name:"getOverlay",args:[r],logPanel:n});return(u,e)=>{const N=U("dweb-navigation-bar");return $(),G(L,null,[b(N,{ref_key:"$navigationBar",ref:y,onStatechange:e[0]||(e[0]=l=>m(l.detail,"change"))},null,512),t("div",E,[t("figure",{class:"icon"},[t("img",{src:M,alt:h})]),t("article",T,[H,b(F,{modelValue:s.value,"onUpdate:modelValue":e[1]||(e[1]=l=>s.value=l),modes:["rgba"]},null,8,["modelValue"]),t("div",I,[t("button",{class:"inline-block rounded-full btn btn-accent",disabled:s.value==null,onClick:e[2]||(e[2]=(...l)=>a(f)&&a(f)(...l))}," Set ",8,K),t("button",{class:"inline-block rounded-full btn btn-accent",onClick:e[3]||(e[3]=(...l)=>a(p)&&a(p)(...l))},"Get")])]),t("article",R,[W,g(t("select",{class:"w-full max-w-xs select",name:"navigationbar-style",id:"navigationbar-style","onUpdate:modelValue":e[4]||(e[4]=l=>i.value=l)},Q,512),[[D,i.value]]),t("div",X,[t("button",{class:"inline-block rounded-full btn btn-accent",disabled:i.value==null,onClick:e[5]||(e[5]=(...l)=>a(k)&&a(k)(...l))}," Set ",8,Y),t("button",{class:"inline-block rounded-full btn btn-accent",onClick:e[6]||(e[6]=(...l)=>a(C)&&a(C)(...l))},"Get")])]),t("article",Z,[tt,g(t("input",{class:"toggle",type:"checkbox",id:"navigationbar-overlay","onUpdate:modelValue":e[7]||(e[7]=l=>d.value=l)},null,512),[[P,d.value]]),t("div",et,[t("button",{class:"inline-block rounded-full btn btn-accent",disabled:d.value==null,onClick:e[8]||(e[8]=(...l)=>a(V)&&a(V)(...l))}," Set ",8,lt),t("button",{class:"inline-block rounded-full btn btn-accent",onClick:e[9]||(e[9]=(...l)=>a(w)&&a(w)(...l))},"Get")])]),t("article",at,[nt,g(t("input",{class:"toggle",type:"checkbox",id:"navigationbar-overlay","onUpdate:modelValue":e[10]||(e[10]=l=>r.value=l)},null,512),[[P,r.value]]),t("div",ot,[t("button",{class:"inline-block rounded-full btn btn-accent",disabled:r.value==null,onClick:e[11]||(e[11]=(...l)=>a(B)&&a(B)(...l))}," Set ",8,st),t("button",{class:"inline-block rounded-full btn btn-accent",onClick:e[12]||(e[12]=(...l)=>a(S)&&a(S)(...l))},"Get")])])]),it,b(A,{ref_key:"$logPanel",ref:n},null,512)],64)}}});export{vt as default};
