import{d as k,r as c,o as w,c as y,a as u,b as e,h as C,u as o,w as O,v as x,F as M,g as A,f as F,ax as T}from"./index.996ff647.js";import{_ as $}from"./vibrate.d24dcb2b.js";import{F as B}from"./FieldLabel.f41e6c98.js";import{t as R,_ as V,d as i}from"./LogPanel.ce2f93b3.js";const E={class:"card glass"},H={class:"card-body"},L=e("h2",{class:"card-title"},"Scanner",-1),N={class:"card-body"},j=e("h2",{class:"card-title"},"get Photo",-1),z=e("option",{value:"PROMPT"},"PROMPT",-1),D=e("option",{value:"CAMERA"},"CAMERA",-1),G=e("option",{value:"PHOTOS"},"PHOTOS",-1),U=[z,D,G],q={class:"justify-end card-actions"},I=e("div",{class:"divider"},"LOG",-1),Y=k({__name:"BarcodeScanning",setup(J){const h="Scanner",a=c(),g=c();let _,p=T,r;w(()=>{_=R(a),r=g.value});const s=c(),v=i(async P=>{var l;const t=P.target;if(t&&((l=t.files)==null?void 0:l[0])){const n=t.files[0];_.info("photo ==> ",n.name,n.type,n.size),s.value=await p.process(n).then(S=>S.text())}},{name:"process",args:[s],logPanel:a}),f=i(async()=>{await p.stop()},{name:"onStop",args:[],logPanel:a}),m=i(async()=>{s.value=await r.startScanning()},{name:"taskPhoto",args:[s],logPanel:a}),d=c("PHOTOS"),b=i(async()=>{s.value=await r.getPhoto({source:d.value})},{name:"getPhoto",args:[s],logPanel:a});return(P,t)=>{const l=F("dweb-barcode-scanning");return A(),y(M,null,[u(l,{ref_key:"$barcodeScannerPlugin",ref:g},null,512),e("div",E,[e("figure",{class:"icon"},[e("img",{src:$,alt:h})]),e("article",H,[L,u(B,{label:"Vibrate Pattern:"},{default:C(()=>[e("input",{type:"file",onChange:t[0]||(t[0]=n=>o(v)(n)),accept:"image/*",capture:""},null,32)]),_:1}),e("button",{class:"inline-block rounded-full btn btn-accent",onClick:t[1]||(t[1]=(...n)=>o(m)&&o(m)(...n))},"scanner"),e("button",{class:"inline-block rounded-full btn btn-accent",onClick:t[2]||(t[2]=(...n)=>o(f)&&o(f)(...n))},"stop")]),e("article",N,[j,O(e("select",{class:"w-full max-w-xs select","onUpdate:modelValue":t[3]||(t[3]=n=>d.value=n)},U,512),[[x,d.value]]),e("div",q,[e("button",{class:"inline-block rounded-full btn btn-accent",onClick:t[4]||(t[4]=(...n)=>o(b)&&o(b)(...n))},"getPhoto")])])]),I,u(V,{ref_key:"$logPanel",ref:a},null,512)],64)}}});export{Y as default};
