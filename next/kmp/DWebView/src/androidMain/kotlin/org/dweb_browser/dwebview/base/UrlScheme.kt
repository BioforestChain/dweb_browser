package org.dweb_browser.dwebview.base

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build

val urlScheme = setOf(
  "about",
  "acap",
  "addbook",
  "afp",
  "afs",
  "aim",
  "applescript",
  "bcp",
  "bk",
  "btspp",
  "callto",
  "castanet",
  "cdv",
  "chrome",
  "chttp",
  "cid",
  "crid",
  "data",
  "dav",
  "daytime",
  "device",
  "dict",
  "dns",
  "doi",
  "dtn",
  "ed2k",
  "eid",
  "enp",
  "fax",
  "feed",
  "file",
  "finger",
  "freenet",
  "ftp",
  "go",
  "gopher",
  "gsiftp",
  "gsm-sms",
  "h323",
  "h324",
  "hdl",
  "hnews",
  "http",
  "https",
  "httpsy",
  "iioploc",
  "ilu",
  "im",
  "imap",
  "info",
  "IOR",
  "ip",
  "ipp",
  "irc",
  "iris.beep",
  "itms",
  "jar",
  "javascript",
  "jdbc",
  "klik",
  "kn",
  "lastfm",
  "ldap",
  "lifn",
  "livescript",
  "lrq",
  "mac",
  "magnet",
  "mailbox",
  "mailserver",
  "mailto",
  "man",
  "md5",
  "mid",
  "mms",
  "mocha",
  "modem",
  "moz-abmdbdirectory",
  "msni",
  "mtqp",
  "mumble",
  "mupdate",
  "myim",
  "news",
  "nltk",
  "nfs",
  "nntp",
  "oai",
  "opaquelocktoken",
  "pcast",
  "phone",
  "php",
  "pop",
  "pop3",
  "pres",
  "printer",
  "prospero",
  "pyimp",
  "rdar",
  "res",
  "rtsp",
  "rvp",
  "rwhois",
  "rx",
  "sdp",
  "secondlife",
  "service",
  "sip",
  "sips",
  "smb",
  "smtp",
  "snews",
  "snmp",
  "soap.beep",
  "soap.beeps",
  "soap.udp",
  "SubEthaEdit",
  "svn",
  "svn+ssh",
  "t120",
  "tag",
  "tann",
  "tcp",
  "tel",
  "telephone",
  "telnet",
  "tftp",
  "thismessage",
  "tip",
  "tn3270",
  "tv",
  "txmt",
  "uddi",
  "urn",
  "uuid",
  "vemmi",
  "videotex",
  "view-source",
  "wais",
  "wcap",
  "webcal",
  "whodp",
  "whois",
  "whois++",
  "wpn",
  "wtai",
  "xeerkat",
  "xfire",
  "xmlrpc.beep",
  "xmlrpc.beeps",
  "xmpp",
  "ymsgr",
  "z39.50r",
  "z39.50s",
)

fun isWebUrlScheme(scheme: String) =
  scheme == "http" || scheme == "https" || urlScheme.contains(scheme) || scheme.startsWith("web+")

fun isSchemeAppInstalled(mContext: Context, uri: Uri): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val list: List<ResolveInfo> = mContext.packageManager.queryIntentActivities(intent, 0)
    val possibleBrowserIntents: List<ResolveInfo> = mContext.packageManager.queryIntentActivities(
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("http://example.com/")
        ), 0
      )
    val excludeIntents: MutableSet<String> = HashSet()
    for (eachPossibleBrowserIntent: ResolveInfo in possibleBrowserIntents) {
      excludeIntents.add(eachPossibleBrowserIntent.activityInfo.name)
    }
    //Check for non browser application
    for (resolveInfo: ResolveInfo in list) {
      if (!excludeIntents.contains(resolveInfo.activityInfo.name)) {
        intent.setPackage(resolveInfo.activityInfo.packageName)
        return true
      }
    }
  } else {
    try {
      // In order for this intent to be invoked, the system must directly launch a non-browser app.
      // Ref: https://developer.android.com/training/package-visibility/use-cases#avoid-a-disambiguation-dialog
      val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER or Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT
        )
      if (intent.resolveActivity(mContext.packageManager) != null) {
        return true
      }
    } catch (e: ActivityNotFoundException) {
      // This code executes in one of the following cases:
      // 1. Only browser apps can handle the intent.
      // 2. The user has set a browser app as the default app.
      // 3. The user hasn't set any app as the default for handling this URL.
      return false
    }
  }
  return false
}