import { node_fs, node_path } from "../deps/node.ts";

export const findDependencyPath = (cwd: string, dependencyName: string): string | null => {
  // 1. 标准化并获取绝对路径，确保处理 '..' 等情况
  let currentPath = node_path.resolve(cwd);

  // 2. 循环向上查找
  while (true) {
    // 3. 构建当前层级的 node_modules 路径
    const potentialNodeModulesPath = node_path.join(currentPath, "node_modules");

    // 4. 构建目标依赖项在当前 node_modules 中的完整路径
    const potentialDependencyPath = node_path.join(potentialNodeModulesPath, dependencyName);

    // 5. 检查目标依赖项是否存在于当前 node_modules 目录中
    if (node_fs.existsSync(potentialDependencyPath)) {
      return potentialDependencyPath;
    }

    // 6. 获取父目录路径
    const parentPath = node_path.dirname(currentPath);

    // 7. 检查是否到达根目录
    //    当父目录和当前目录相同时，表示已到达根目录（例如 '/' 或 'C:\'）
    if (parentPath === currentPath) {
        // 已经到达根目录，没有找到
        return null;
    }

    // 8. 移动到父目录，进行下一次迭代
    currentPath = parentPath;
  }
};
