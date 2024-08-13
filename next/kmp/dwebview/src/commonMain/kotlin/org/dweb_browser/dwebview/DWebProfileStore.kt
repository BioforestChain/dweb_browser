package org.dweb_browser.dwebview

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.base64UrlBinary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.helper.utf8ToBase64UrlString

expect suspend fun getDwebProfileStoreInstance(): DWebProfileStore

interface DWebProfileStore {
  suspend fun getAllProfileNames(): List<ProfileName>
  suspend fun deleteProfile(name: ProfileName): Boolean
}


sealed interface ProfileName {
  val key: String
  val isIncognito: Boolean
  val mmid: MMID?
  val profile: String?

  companion object {
    fun parse(profileNameKey: String): ProfileName {
      return if (profileNameKey == "*") {
        NoProfileName()
      } else if (profileNameKey.contains("@") && profileNameKey.endsWith(IncognitoSuffix)) {
        ProfileIncognitoNameV1.parse(profileNameKey)
      } else if (profileNameKey.contains("/")) {
        ProfileNameV1.parse(profileNameKey)
      } else {
        ProfileNameV0.parse(profileNameKey)
      }
    }
  }
}


class NoProfileName : ProfileName {
  override val key: String = "*"
  override val isIncognito = false
  override val mmid = null
  override val profile = null
}

class ProfileNameV0(
  override val mmid: String,
  override val key: String = mmid,
) : ProfileName {
  companion object {
    fun parse(profileNameKey: String): ProfileNameV0 {
      return ProfileNameV0(profileNameKey)
    }
  }

  override val profile = null
  override val isIncognito = false
}

class ProfileNameV1(
  override val mmid: String,
  override val profile: String,
  override val key: String = "${mmid}/${profile.utf8ToBase64UrlString}",
) : ProfileName {
  companion object {
    fun parse(profileNameKey: String): ProfileNameV1 {
      val splitIndex = profileNameKey.indexOf("/")
      val mmid = profileNameKey.substring(0, splitIndex)
      val profile = profileNameKey.substring(splitIndex + 1).base64UrlBinary.utf8String
      return ProfileNameV1(mmid, profile, profileNameKey)
    }
  }

  override val isIncognito = false
}

internal const val IncognitoSuffix = ".incognito"

class ProfileIncognitoNameV1(
  val profileName: ProfileName,
  val sessionId: String,
  override val key: String = "$profileName@${sessionId.utf8ToBase64UrlString}$IncognitoSuffix",
) : ProfileName {
  companion object {
    fun parse(profileNameKey: String): ProfileIncognitoNameV1 {
      val splitIndex = profileNameKey.indexOf("@")
      val profileName = ProfileNameV1.parse(profileNameKey.substring(0, splitIndex))
      val sessionId = profileNameKey.substring(
        splitIndex + 1, profileNameKey.length - IncognitoSuffix.length
      ).base64UrlBinary.utf8String
      return ProfileIncognitoNameV1(profileName, sessionId, profileNameKey)
    }

    fun from(profileName: ProfileName, sessionId: String) = when {
      profileName.isIncognito -> profileName
      else -> ProfileIncognitoNameV1(profileName, sessionId)
    }
  }


  override val isIncognito = true
  override val mmid get() = profileName.mmid
  override val profile get() = profileName.profile
}
