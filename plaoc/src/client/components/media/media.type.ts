export interface MediaOption {
  file?: File; // 保存单张
  // files?: FileList; // 保存多张
  saveLocation?: string; // 保存的相册名称（android only）
}
