using UIKit;

namespace DwebBrowser.Platforms.iOS;

public class Program
{
    // This is the main entry point of the application.
    static void Main(string[] args)
    {
        /**
         * workerThreads: 
         *   用于执行线程池中的工作项，包括处理非异步的 CPU 密集型任务，这些线程负责执行通过 ThreadPool.QueueUserWorkItem、Task.Run 或其他类似方法提交的工作项。
         * 
         * completionPortThreads:
         *   是一种异步 I/O 模型，用于高效地处理大量的 I/O 操作，如文件 I/O、网络 I/O 等。由操作系统和线程池自动完成的，无法通过特定的启动方式来创建和控制。
         *
         */
        ThreadPool.SetMinThreads(75, 50);
        _ = MicroService.Start();
        // if you want to use a different Application Delegate class from "AppDelegate"
        // you can specify it here.
        UIApplication.Main(args, null, typeof(AppDelegate));
    }
}
