//@ts-check
const http = require("node:http")
http.createServer((req, res) => {
    res.setHeader('Content-Type', 'text/html');
    res.write("<h1 id='xx'>asdasd</h1>") 
    setInterval(() => { 
        res.write(`<script>xx.innerHTML = "${Math.random()}"</script>`) 
    }, 1000)
}).listen(12002)