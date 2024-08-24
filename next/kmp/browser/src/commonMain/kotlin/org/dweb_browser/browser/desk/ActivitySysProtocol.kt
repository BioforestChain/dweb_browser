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
          ActivityItem(
            owner = getRemoteRuntime(),
            leadingIcon = request.queryAsOrNull("leadingIcon") ?: ActivityItem.NoneIcon,
            trailingIcon = request.queryAsOrNull("trailingIcon") ?: ActivityItem.NoneIcon,
            centerTitle = request.queryAs("centerTitle"),
            bottomActions = request.queryAsOrNull("bottomActions") ?: emptyList(),
          )
        )
      },
      "/updateActivity" bind PureMethod.GET by defineBooleanResponse {
        activityController.update(
          owner = getRemoteRuntime(),
          id = request.query("id"),
          leadingIcon = request.queryAsOrNull("leadingIcon"),
          trailingIcon = request.queryAsOrNull("trailingIcon"),
          centerTitle = request.queryAsOrNull("centerTitle"),
          bottomActions = request.queryAsOrNull("bottomActions"),
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