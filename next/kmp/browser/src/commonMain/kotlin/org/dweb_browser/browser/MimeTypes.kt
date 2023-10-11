package org.dweb_browser.browser


/**
 * See [Media Types](https://www.iana.org/assignments/media-types/media-types.xhtml) of IANA.
 */
object MimeTypes {

  fun getMimeTypeFromExtension(url:String): String {
    val extIndex = url.lastIndexOf(".")
    if (extIndex == -1) throw Exception("The url passed is not a file！")
    val ext = url.substring(extIndex)
    println("getMimeTypeFromExtension ext=> $ext")
    return getMimeType(ext)
  }

  fun getMimeType(ext: String): String {
    return map[ext] ?: "*/*"
  }

  private val map = mapOf(
    "abw" to "application/x-abiword", // AbiWord document
    "arc" to "application/x-freearc", // Archive document (multiple files embedded)

    "atom" to "application/atom+xml", // Atom Documents
    "atomcat" to "application/atomcat+xml", // Atom Category Documents

    "azw" to "application/vnd.amazon.ebook", // Amazon Kindle eBook format
    "mpkg" to "application/vnd.apple.installer+xml", // Apple Installer Package

    "csh" to "application/x-csh", // C-Shell script
    "es" to "application/ecmascript", // ECMAScript

    "epub" to "application/epub+zip", // Electronic publication (EPUB)
    "gz" to "application/gzip", // GZip Compressed Archive

    "jar" to "application/java-archive", // Java Archive (JAR)
    "js" to "application/javascript", // JavaScript

    "json" to "application/json", // JSON format
    "jsonld" to "application/ld+json", // JSON-LD format

    "xls" to "application/vnd.ms-excel", // Microsoft Excel
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Microsoft Excel (OpenXML)

    "eot" to "application/vnd.ms-fontobject", // MS Embedded OpenType fonts
    "ppt" to "application/vnd.ms-powerpoint", // Microsoft PowerPoint

    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation", // Microsoft PowerPoint (OpenXML)
    "doc" to "application/msword", // Microsoft Word

    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // Microsoft Word (OpenXML)
    "vsd" to "application/vnd.visio", // Microsoft Visio

    "bin" to "application/octet-stream", // Any kind of binary data
    "ogx" to "application/ogg", // OGG

    "odp" to "application/vnd.oasis.opendocument.presentation", // OpenDocument presentation document
    "ods" to "application/vnd.oasis.opendocument.spreadsheet", // OpenDocument spreadsheet document

    "odt" to "application/vnd.oasis.opendocument.text", // OpenDocument text document
    "pdf" to "application/pdf", // Adobe Portable Document Format (PDF)

    "php" to "application/x-httpd-php", // Hypertext Preprocessor (Personal Home Page)
    "p10" to "application/pkcs10", // PKCS #10

    "p7m" to "application/pkcs7-mime", // PKCS #7 MIME
    "p7s" to "application/pkcs7-signature", // PKCS #7 Signature

    "p8" to "application/pkcs8", // PKCS #8
    "p12" to "application/x-pkcs12", // PKCS #12

    "ps" to "application/postscript", // PostScript
    "rar" to "application/vnd.rar", // RAR archive

    "rdf" to "application/rdf+xml", // RDF/XML
    "rtf" to "application/rtf", // Rich Text Format (RTF)

    "7z" to "application/x-7z-compressed", // 7-zip archive
    "sh" to "application/x-sh", // Bourne shell script

    "smil" to "application/smil+xml", // SMIL documents
    "sql" to "application/sql", // SQL

    "swf" to "application/x-shockwave-flash", // Small web format (SWF) or Adobe Flash document
    "tar" to "application/x-tar", // Tape Archive (TAR)

    "bz" to "application/x-bzip", // BZip Archive
    "bz2" to "application/x-bzip2", // BZip2 Archive

    "xhtml" to "application/xhtml+xml", // XHTML
    "xml" to "application/xml", // XML

    "dtd" to "application/xml-dtd", // XML DTD
    "xsl" to "application/xslt+xml", // XSLT Document

    "xul" to "application/vnd.mozilla.xul+xml", // XUL
    "zip" to "application/zip", // Zip Archive

    "aac" to "audio/aac", // AAC audio
    "mid" to "audio/midi", // Musical Instrument Digital Interface (MIDI)

    "midi" to "audio/x-midi", // Musical Instrument Digital Interface (MIDI)
    "mka" to "audio/x-matroska", // Matroska Multimedia Container

    "mp3" to "audio/mpeg", // MP3 audio
    "mp4" to "audio/mp4", // MP4 audio
    "oga" to "audio/ogg", // OGG audio

    "opus" to "audio/opus", // Opus audio
    "wav" to "audio/wav", // Waveform Audio Format

    "weba" to "audio/webm", // WEBM audio

    "bmp" to "image/bmp", // Windows OS/2 Bitmap Graphics
    "gif" to "image/gif", // Graphics Interchange Format (GIF)

    "ico" to "image/vnd.microsoft.icon", // Icon format
    "jpg" to "image/jpeg", // JPEG images

    "png" to "image/png", // Portable Network Graphics
    "svg" to "image/svg+xml", // Scalable Vector Graphics (SVG)

    "tif" to "image/tiff", // Tagged Image File Format (TIFF)
    "webp" to "image/webp", // WEBP image

    "heic" to "image/heic", // HEIC image
    "heif" to "image/heif", // HEIF image

    "avif" to "image/avif", // AVIF image
    "css" to "text/css", // Cascading Style Sheets (CSS)

    "csv" to "text/csv", // Comma-separated values (CSV)
    "html" to "text/html", // HyperText Markup Language (HTML)

    "ics" to "text/calendar", // iCalendar format
    "mjs" to "text/javascript", // JavaScript

    "txt" to "text/plain", // Text, (generally ASCII or ISO 8859-n)
    "sgml" to "text/sgml", // Standard Generalized Markup Language

    "yml" to "text/yaml", // YAML
    "avi" to "video/x-msvideo", // AVI: Audio Video Interleave

    "3gp" to "video/3gpp", // 3GPP audio/video container
    "3g2" to "video/3gpp2", // 3GPP2 audio/video container

    "mkv" to "video/x-matroska", // Matroska Multimedia Container
    "mp4" to "video/mp4", // MP4 video

    "mpg" to "video/mpeg", // MPEG Video
    "ts" to "video/mp2t", // MPEG transport stream

    "ogv" to "video/ogg", // OGG video
    "mov" to "video/quicktime", // QuickTime

    "webm" to "video/webm", // WEBM video
    "otf" to "font/otf", // OpenType font

    "ttf" to "font/ttf", // TrueType Font
    "woff" to "font/woff", // Web Open Font Format (WOFF)

    "woff2" to "font/woff2" // Web Open Font Format (WOFF)

  )

}