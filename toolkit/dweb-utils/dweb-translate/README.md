# dweb-translate

dweb 自动化翻译工具。

> 翻译标准使用[ISO_639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes).

### 示例

```bash
dwebt  --sourcelang=zh --targetlang=en,ja --outdir=i18ndir [--server-api-key=]
```

<!-- --mode=manifest *.json -->

以上参数都可以写在配置文件中，我们会调用翻译服务进行，翻译服务可以进行配置，也可以用命令行参数来支持。
如果没有填写`或者`,会默认读取 .dwebtranslaterc/dwebtranslaterc.json 文件。

输出的文件夹中，会有一个 manifest.i18n.json 的文件，它会枚举这个文件夹中的其它多语言文件。

### 选项

- `[...files]` :需要翻译的文件。
- `-f, --from <lang>` : 源文件的语言。
- `-t, --to <lang>` : 翻译的目标语言。
- `-o, --outDir [dir]` : 输出翻译后的文件的目录。
- `-c, --config <file>` : 指定用于翻译的配置文件。
<!-- - `-m, --mode` : 意味着翻译模式，就是将 json 文件当成 manifest 格式来处理，那么就会只针对该文件中必要的字段进行翻译。 -->
