import com.dd.plist.NSDictionary
import com.dd.plist.XMLPropertyListParser
import com.dd.plist.XMLPropertyListWriter
import java.nio.file.Path

fun infoPlistFileReplace(infoPlistFile: Path, replaceTargetPath: Path, infoPlistNSDictionary: NSDictionary) {
  val infoPlistOb = XMLPropertyListParser.parse(infoPlistFile) as NSDictionary

  infoPlistNSDictionary.forEach { (key, value) ->
    infoPlistOb[key] = value
  }

  XMLPropertyListWriter.write(infoPlistOb, replaceTargetPath)
}