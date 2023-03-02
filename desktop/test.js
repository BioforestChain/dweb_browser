  
const fsPromises = require("node:fs/promises");
const path  = require("path")

async function readFile(){
    const result = await fsPromises.readFile(path.resolve(__dirname, "./src/sys/jmm-metadata/assets/index.html"))
    return new TextDecoder().decode(result)
}

(async () => {
    console.log(await readFile())
})()