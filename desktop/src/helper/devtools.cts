// 开发工具
export class Log{
    log(str: string){
        console.log(str)
    }

    red(str: string){
        console.log(`\x1B[31m%s\x1B[0m`, str)
    }

    green(str: string){
        console.log(`\x1B[32m%s\x1B[0m`, str)
    }

    yellow(str: string){
        console.log(`\x1B[33m%s\x1B[0m`, str)
    }

    blue(str: string){
        console.log(`\x1B[34m%s\x1B[0m`, str)
    }

    // 品红色
    magenta(str: string){
        console.log(`\x1B[35m%s\x1B[0m`, str)
    }

    cyan(str: string){
        console.log(`\x1B[36m%s\x1B[0m`, str)
    }

    grey(str: string){
        console.log(`\x1B[36m%s\x1B[0m`, str)
    }
}
export const log = new Log()
 