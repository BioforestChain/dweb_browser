const express = require('express');
const fs = require('fs')
const path = require('path')

const app = express();
      app.get('/', routeIndex);
      app.get('/index.js', routerIndexJs)

const server = app.listen(8080, '127.0.0.1', () => {
  console.log(`服务器运行在 http://${server.address().address}:${server.address().port}`);
});

/**
 * 处理 index.html 请求
 * @param {*}} req 
 * @param {*} res 
 */
async function routeIndex(req, res){
  const indexHtmlPath = path.resolve(__dirname, "./src/index.html")
  const readableStream = fs.createReadStream(indexHtmlPath,{encoding: "utf-8"})
  readableStream.on('open', () => {
    res.setHeader('Content-Type', 'text/html');
    readableStream.pipe(res);
  });
  readableStream.on('end', () => {
    res.end() 
  })
}

/**
 * 处理 /index.js 服务
 * @param {*} req 
 * @param {*} res 
 */
async function routerIndexJs(req, res){
  const indexJsPath = path.resolve(__dirname, "./src/index.js");
  const readableStream = fs.createReadStream(indexJsPath, {encoding: "utf-8"});
  readableStream.on("open", () => {
    res.setHeader('Content-Type', "text/javascript");
    readableStream.pipe(res)
  })
  readableStream.on('end', () => {
    res.end()
  })
}