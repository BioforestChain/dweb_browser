package org.dweb_browser.sys.clipboard

import org.dweb_browser.helper.base64UrlString
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual class ClipboardManage {
  private val clipboard by lazy { Toolkit.getDefaultToolkit().systemClipboard; }


  actual fun writeText(
    content: String,
    label: String?
  ) = tryWriteClipboard {
    clipboard.setContents(StringSelection(content), null)
  }

  class ImageSelection(private val image: BufferedImage) : Transferable {
    override fun getTransferData(flavors: DataFlavor): Any {
      return if (isDataFlavorSupported(flavors)) image else throw UnsupportedFlavorException(flavors)
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
      return arrayOf(DataFlavor.imageFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
      return DataFlavor.imageFlavor.equals(flavor)
    }
  }

  actual fun writeImage(
    base64DataUri: String,
    label: String?
  ) = tryWriteClipboard {

    val (imageData) = splitBase64DataUriToFile(base64DataUri)
    val image = ImageIO.read(
      imageData.inputStream()
    )
    clipboard.setContents(ImageSelection(image), null)
  }

  actual fun writeUrl(
    url: String,
    label: String?
  ) = writeText(url, label)

  private val emptySelection = object : Transferable {
    override fun getTransferData(flavors: DataFlavor?): Any {
      throw UnsupportedFlavorException(flavors)
    }

    override fun getTransferDataFlavors() = emptyArray<DataFlavor>()

    override fun isDataFlavorSupported(p0: DataFlavor?) = false
  }

  actual fun clear(): Boolean {
    clipboard.setContents(emptySelection, null)
    return true
  }


  actual fun read(): ClipboardData {
    var value = ""
    var type = ""
    val content = clipboard.getContents(null)
    if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      type = "text/plain"
      value = (content.getTransferData(DataFlavor.stringFlavor) as String?) ?: ""
    } else if (content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      content.getTransferData(DataFlavor.imageFlavor).also {
        if (it is BufferedImage) {
          val baos = ByteArrayOutputStream();
          ImageIO.write(it, "png", baos)
          type = "image/png"
          value = baos.toByteArray().base64UrlString
        }
      } as BufferedImage
    }
    return ClipboardData(value, type)
  }
}