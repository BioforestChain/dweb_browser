package org.dweb_browser.browser.bookmark.BookPage

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.browser.bookmark.Bookmark

/**
 * 书签页的数据管理
 */
class BookPageModel {
  /**
   * 是否是编辑模式
   */
  val editMode = mutableStateOf(false)
}

val LocalBookPageModel = compositionLocalOf { BookPageModel() }

class BookmarkEditModel(val data: MutableState<Bookmark>) {
  constructor(data: Bookmark) : this(mutableStateOf(data))

  /**
   * 是否显示菜单栏
   */
  val showMoreOptions = mutableStateOf(false)

  /**
   * 是否显示编辑对话框
   */
  val showEditDialog = mutableStateOf(false)
}


val LocalBookmarkEditModel =
  compositionLocalOf<BookmarkEditModel> { throw Exception(" BookmarkEditModel") }
