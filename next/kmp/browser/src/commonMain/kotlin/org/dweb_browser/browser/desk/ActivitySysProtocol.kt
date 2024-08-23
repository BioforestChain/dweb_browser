package org.dweb_browser.browser.desk

import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull

suspend fun DeskNMM.DeskRuntime.activityProtocol() {
  val activityController = deskController.activityController
  protocol("activity.sys.dweb") {
    routes(
      /// 请求一次实时活动
      "/requestActivity" bind PureMethod.GET by defineStringResponse {
        activityController.request(
          owner = getRemoteRuntime(),
          icon = request.queryAsOrNull("icon") ?: ActivityItem.NoneIcon,
          content = request.queryAs("content"),
          action = request.queryAsOrNull("action") ?: ActivityItem.NoneAction,
        )
      },
      "/updateActivity" bind PureMethod.GET by defineBooleanResponse {
        activityController.update(
          owner = getRemoteRuntime(),
          id = request.query("id"),
          icon = request.queryAsOrNull("icon"),
          content = request.queryAsOrNull("content"),
          action = request.queryAsOrNull("action"),
        )
      },
      "/endActivity" bind PureMethod.GET by defineBooleanResponse {
        activityController.end(
          owner = getRemoteRuntime(),
          id = request.query("id"),
        )
      },
    )
  }
}