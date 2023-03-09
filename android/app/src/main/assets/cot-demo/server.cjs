const express = require('express');
const fs = require('fs')
const path = require('path')

const app = express();
      app.get('/', routeIndex);
      app.get('/index.js', routerIndexJs)
      app.put('/api', routeApi)

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

/**
 * 处理 /api 的fetch 服务
 * @param {*} req 
 * @param {*} res 
 */
async function routeApi(req, res){
  switch(req.query.app_id){
    case "statusbar.sys.dweb":
      onRouteApiStatusbar(req, res);
      break;
    default: onRouteApiDefault(req, res)

  }
  console.log("query: ", req.query)

}

async function onRouteApiStatusbar(req, res){
  switch(req.query.action){
    case "set_style":
      onRouteApiStatusbarSetStyle(req, res);
      break;
    default:
      onRouteApiStatusbarDefault(req, res);
      break;
  }
}

async function onRouteApiStatusbarSetStyle(req, res){
  const value = req.query.value;
  if(value === "LIGHT" || value === "DARK" || value === "DEFAULT"){
    res.setHeader('content-type', "text/plain")
    res.statusCode = 200;
    res.send('ok')
    res.end()
    return;
  }

  res.setHeader('content-type', "text/plain");
  res.statusCode = 400;
  res.send('非法的 value ')
  res.end();
}

async function onRouteApiStatusbarDefault(req, res){
  res.setHeader('content-type', "text/plain");
  res.statusCode = 400;
  res.send('非法的 action ')
  res.end();
}




async function onRouteApiDefault(req, res){
  res.setHeader("content-type", "text/plain")
  res.statusCode = 404
  res.send('not found')
  res.end()
}