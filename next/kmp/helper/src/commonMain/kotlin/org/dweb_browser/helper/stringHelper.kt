package org.dweb_browser.helper

public fun String.removeInvisibleChars(): String = replace(Regex("[\\p{C}\\p{Z}&&[^\\p{Zs}]]"), "")

public fun String.humanTrim(): String = trim(
  // 零宽字符：
  '\u200B',//零宽空格 (ZWSP)
  '\u200C',//零宽非连接符 (ZWNJ)
  '\u200D',//零宽连接符 (ZWJ)

  // 常见的空白字符：

  '\u0020',// 普通空格 (Space)
  '\u00A0',// 不间断空格 (Non-breaking space)
  '\u1680',// 赡养符 (Ogham space mark)
  '\u2000',// 到 \u200A 一系列空格（各种宽度的空格）
// 格式控制字符（这些字符主要用于控制文本格式，通常在现代文本中不再使用，但在某些场景中仍然可能出现）：

  '\u200B',// 零宽空格（Zero Width Space）
  '\u200C',// 零宽非连接符（Zero Width Non-Joiner）
  '\u200D',// 零宽连接符（Zero Width Joiner）
  '\u200E',// 左到右标记（Left-to-Right Mark, LRM）
  '\u200F',// 右到左标记（Right-to-Left Mark, RLM）
  '\u202A',// 到 \u202E 一些文本方向控制字符（如：右到左方向标记、强制方向标记等）
// 换行符与回车符：

  '\u000A',// 换行符（Line Feed, LF）
  '\u000D',// 回车符（Carriage Return, CR）
  '\u2028',// 行分隔符（Line Separator）
  '\u2029',// 段分隔符（Paragraph Separator）

)