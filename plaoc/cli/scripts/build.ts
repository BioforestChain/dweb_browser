
const target = [
  "x86_64-unknown-linux-gnu",
  "x86_64-pc-windows-msvc",
  "x86_64-apple-darwin",
  "aarch64-apple-darwin",
];

target.map(async (os) => {
  let output = "jmm";
  if (os.includes("windows")) {
    output = "jmm.exe"
  }

  const command = new Deno.Command(
    Deno.execPath(),{
      args:[
        "compile",
        `--output=./bin/${os}-${output}`,
        `--target=${os}`,
        "./src/index.ts"
      ]
    }
  );
  const { stderr } = await command.output();
  
  console.error(new TextDecoder().decode(stderr));
});
