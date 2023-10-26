package org.dweb_browser.shared.microService.sys.notification

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAttachment
import platform.UserNotifications.UNNotificationPresentationOptionAlert
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

actual class NotifyManager actual constructor() {

    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    init {
        addNotificationProtocol()
    }
    private fun addNotificationProtocol() {

        center.delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {

            override fun userNotificationCenter(
                center: UNUserNotificationCenter,
                willPresentNotification: UNNotification,
                withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
            ) {
                withCompletionHandler(UNNotificationPresentationOptionAlert)
            }

            override fun userNotificationCenter(
                center: UNUserNotificationCenter,
                didReceiveNotificationResponse: UNNotificationResponse,
                withCompletionHandler: () -> Unit
            ) {
                val notification = didReceiveNotificationResponse.notification
                print(notification.request.content.userInfo)
                withCompletionHandler()
            }
        }
    }
    @OptIn(ExperimentalForeignApi::class)
    actual fun createNotification(
        title: String,
        subTitle: String?,
        text: String,
        androidIcon: Int?,
        iosIcon: String?,
        bigText: String?,
        intentUrl: String?,
        commonChannelType: CommonChannelType,
        intentType: PendingIntentType
    ) {

        var content = UNMutableNotificationContent().apply {
            setTitle(title)
            setSubtitle(title)
            setBody(bigText ?: text)

            if (iosIcon != null) {
                val url = NSURL.URLWithString(iosIcon!!)
                if (url != null) {
                    var attachment = UNNotificationAttachment.attachmentWithIdentifier("ThumbnailImage", url!!, null,null)
                    setAttachments(listOf(attachment))
                }
            }
        }

        val date = NSDate.dateWithTimeIntervalSinceNow(1.0)
        val componentsSet =
            (NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond)

        val triggerDate = NSCalendar.currentCalendar.components(
            componentsSet, date
        )
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            triggerDate, repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(title, content, trigger)

        val withCompletionHandler: (NSError?) -> Unit = { error: NSError? ->
            println("Notification completed with: $error")
        }

        val block = object : (Boolean, NSError?) -> Unit {
            override fun invoke(p1: Boolean, p2: NSError?) {}
        }

        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert, block)
        center.addNotificationRequest(request, withCompletionHandler)
    }
}

