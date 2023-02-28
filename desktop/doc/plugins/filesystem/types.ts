export type $Filesystem = typeof import('./filesystem.dev')['default'];
export {
  Directory as DIRECTORY,
  Encoding as ENCODING,
  type AppendFileOptions as $AppendFileOptions,
  type DeleteFileOptions as $DeleteFileOptions,
  type MkdirOptions as $MkdirOptions,
  type PermissionStatus as $PermissionStatus,
  type ReaddirOptions as $ReaddirOptions,
  type ReadFileOptions as $ReadFileOptions,
  type ReadFileResult as $ReadFileResult,
  type StatOptions as $StatOptions,
  type WriteFileOptions as $WriteFileOptions,
  type WriteFileResult as $WriteFileResult,
} from '@capacitor/filesystem';
