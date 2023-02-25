const  fsPromises = require("node:fs/promises")


fsPromises
.readFile("./src/sys/statusbar/assets/index.html")
.then(content => {
    console.log('content: ', content)
    let _cotnent = new TextDecoder().decode(content)
    console.log(_cotnent)

})
.catch(err => console.log('error: ', err))