const fs = require('fs')

const a = {
    a: "aaa"
}

const b = {
    a:'aaa',
    b: 'bbb',
    c: 'ccc'
}
const main = async ()=> {
     fs.writeFile(
        "./test.json",
        JSON.stringify(b),
        {flag: "w"},
        () => {
            console.log('写入1')
             fs.writeFile(
                "./test.json",
                JSON.stringify(a),
                {flag: "w"},
                () => console.log('写入2')
            )
        }
    )
    
    
}

main()