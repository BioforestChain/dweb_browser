package org.dweb_browser.sys.biometrics

import org.dweb_browser.helper.PromiseOut

internal class BiometricsController {
  companion object {
    val biometricsController = BiometricsController()
  }

  var activity: BiometricsActivity? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityTask = PromiseOut()
      } else {
        activityTask.resolve(value)
      }
    }

  private var activityTask = PromiseOut<BiometricsActivity>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()
}

