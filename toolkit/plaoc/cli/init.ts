import { Checkbox, Command, Input, prompt,  } from "./deps/cliffy.ts";
import { Ajv } from "./deps/ajv.ts";
import { getManifestFilePath } from "./helper/util.ts";
import manifest_schema from "./manifest/manifest-schema.json" assert { type: "json" };
import manifest_json from "./manifest/manifest-template.json" assert { type: "json" };
import fs from "node:fs";

export const doInitCommand = new Command()
  .arguments("[cwd:string]")
  .description("Init manifest.json")
  .option("-y --yes", "Skip manifest.json interactive input.")
  .action(async (options, arg) => {
    const manifest = manifest_json;
    if(!options.yes) {
      const result = await prompt([
        {
          name: "id",
          message: `
  Please enter a string adhering to the '{name}.{host}.dweb' format:
  - {name} should be the name you want to use.
  - {host} should be the corresponding host name.
  For instance, if you choose 'myapp' for the name and 'www.dweb-browser.com' for the host, you should input 'myapp.www.dweb-browser.com.dweb'.
  `,
          type: Input,
        },
        {
          name: "name",
          message: "Please enter app name.",
          type: Input,
        },
        {
          name: "short_name",
          message: "Please enter app short name.",
          type: Input,
        },
        {
          name: "author",
          message: `Please enter one or multiple author name, separated by commas. 
        For instance, if you are inputting multiple authors, it should look like 'author1,author2,author3'.`,
          type: Input,
        },
        {
          name: "categories",
          message: `Please select one or multiple categories. Use the space key to select, and press the enter key to confirm your selection.`,
          type: Checkbox,
          minOptions: 1,
          options: ["application"],
        },
      ]);
      
      manifest.id = result.id?.length == 0 ? manifest.id : result.id!;
      manifest.name = result.name?.length == 0 ? manifest.name : result.name!;
      manifest.short_name = result.short_name?.length == 0 ? manifest.short_name : result.short_name!;
      manifest.categories = result.categories?.length == 0 ? manifest.categories : result.categories!;
      manifest.author = result.author?.length == 0 ? manifest.author : result.author!.split(",");
    }
    
    
    const ajv = new Ajv();
    const validate = ajv.compile(manifest_schema);
    
    if(validate(manifest)) {
      fs.writeFileSync(getManifestFilePath(arg), JSON.stringify(manifest, null, 2));
    }
  });
