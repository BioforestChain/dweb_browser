// 全局声明一个 .html 模块
declare module "*.html" {
    const content: string;
    export default content;
}