import node_crypto from "node:crypto";
import node_fs from "node:fs";
import node_os from "node:os";
import node_path from "node:path";
import node_url from "node:url";

export {
  //
  node_crypto,
  node_fs,
  node_os,
  node_path,
  node_url,
};

export const createHash = node_crypto.createHash;
export const fileURLToPath = node_url.fileURLToPath;
export const pathToFileURL = node_url.pathToFileURL;
