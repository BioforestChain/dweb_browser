package org.dweb_browser.sys.device

expect object DeviceManage {

  /**
   * 增加 uuid 参数的目的是为了 android 之前已经在用的 uuid 保存到文件后，卸载导致权限不足无法获取旧数据问题
   * 所以这边改造成“文件夹名称”后，需要考虑已经存在uuid的数据需要构建文件夹操作，而特地添加了该信息
   */
  suspend fun deviceUUID(uuid: String? = null): String

  fun deviceAppVersion(): String
}