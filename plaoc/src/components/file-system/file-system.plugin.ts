import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type {
  GetUriResult,
  WriteFileResult,
  GetUriOptions,
  WriteFileOptions,
} from "./file-system.type.ts";

export class FileSystemPlugin extends BasePlugin {
  readonly tagName = "dweb-file-system";
  constructor() {
    super("file.sys.dweb");
  }

  @bindThis
  async writeFile(options: WriteFileOptions): Promise<WriteFileResult> {
    const result = await this.fetchApi("/writeFile", {
      search: options,
    }).text();
    return {
      uri: result,
    };
  }

  @bindThis
  async getUri(options: GetUriOptions): Promise<GetUriResult> {
    const result = await this.fetchApi("/getUri", {
      search: options,
    });
    return {
      uri: await result.json(),
    };
  }
}

export const fileSystemPlugin = new FileSystemPlugin();
