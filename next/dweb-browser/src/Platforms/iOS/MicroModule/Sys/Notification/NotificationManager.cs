using Foundation;
using UserNotifications;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Notification;

public static class NotificationManager
{
    static readonly Debugger Console = new("NotificationManager");

    public enum ChannelType
    {
        Default,
        Important
    }

    public static async Task CreateNotification(NotificationOptions options)
    {
        // 构造消息体
        var content = new UNMutableNotificationContent()
        {
            Title = options.title,
            Subtitle = options.subTitle ?? "",
            Body = options.bigText ?? options.text,
            Sound = UNNotificationSound.Default,
        };

        // 添加ThumbnailImage
        if (options.smallIcon is not null)
        {
            var attachmentOptions = new UNNotificationAttachmentOptions();

            var attachment = UNNotificationAttachment.FromIdentifier(
                "ThumbnailImage",
                NSUrl.FromFilename(options.smallIcon),
                attachmentOptions,
                out var error);

            if (error is not null)
            {
                Console.Log("CreateNotification", error.LocalizedDescription);
                throw new Exception(error.LocalizedDescription);
            }

            if (attachment is not null)
            {
                content.Attachments = new UNNotificationAttachment[] { attachment };
            }
        }

        // 构造触发器
        var trigger = UNTimeIntervalNotificationTrigger.CreateTrigger(1, false);

        // 构造请求
        var request = UNNotificationRequest.FromIdentifier(Guid.NewGuid().ToString(), content, trigger);

        await UNUserNotificationCenter.Current.AddNotificationRequestAsync(request);
    }
}

public sealed record NotificationOptions(
    string title,
    string text,
    string? smallIcon,
    string? subTitle,
    string? bigText,
    int? badge,
    NotificationManager.ChannelType channelType);
