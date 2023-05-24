
const { protocol } = require('electron');
 
try{
  protocol.registerSchemesAsPrivileged([
    { scheme: 'http', privileges: { bypassCSP: true, standard: true, secure: true, stream: true }},
    { scheme: 'https', privileges: { bypassCSP: true, standard: true, secure: true, stream: true }},
  ])
}catch(err){
  console.log('err: ', err)
}



const { dns } = require("./dist/main.cjs");
dns.bootstrap();
