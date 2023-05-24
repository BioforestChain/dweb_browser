// 开发工具
export class Log {
    log(str) {
        console.log(str);
    }
    red(str) {
        console.log(`\x1B[31m%s\x1B[0m`, str);
    }
    green(str) {
        console.log(`\x1B[32m%s\x1B[0m`, str);
    }
    yellow(str) {
        console.log(`\x1B[33m%s\x1B[0m`, str);
    }
    blue(str) {
        console.log(`\x1B[34m%s\x1B[0m`, str);
    }
    // 品红色
    magenta(str) {
        console.log(`\x1B[35m%s\x1B[0m`, str);
    }
    cyan(str) {
        console.log(`\x1B[36m%s\x1B[0m`, str);
    }
    grey(str) {
        console.log(`\x1B[36m%s\x1B[0m`, str);
    }
}
export const log = new Log();
