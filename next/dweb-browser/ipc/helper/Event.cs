
using System;

namespace ipc.helper;

//public delegate Task OnMessageHandler<T1, T2>(T1 ipcMessage, T2 ipc);
//public delegate Task OnSingleMessageHandler<T>(T ipcMessage);
//public delegate void OnSimpleMessageHandler();

//public delegate Task OnMessageParamsHandler(params object[]? args);

//public class ParamsEvent
//{
//    public event OnMessageParamsHandler OnMessage;

//    public void Listen(OnMessageParamsHandler cb) => OnMessage += cb;

//    public void Emit(params object[]? args)
//    {
//        if (OnMessage is null) return;
//        foreach (OnMessageParamsHandler cb in OnMessage.GetInvocationList().Cast<OnMessageParamsHandler>())
//        {
//            cb(args);
//        }
//    }

//    public void Remove(OnMessageParamsHandler cb) => OnMessage -= cb;

//    public void Clear()
//    {
//        if (OnMessage is null) return;
//        foreach (OnMessageParamsHandler cb in OnMessage.GetInvocationList().Cast<OnMessageParamsHandler>())
//        {
//            Remove(cb);
//        }
//    }
//}



public delegate Task Signal(Signal self);
public delegate Task Signal<T1>(T1 arg1, Signal<T1> self);
public delegate Task Signal<T1, T2>(T1 arg1, T2 arg2, Signal<T1, T2> self);

public static class SignalHelper
{
    public static async Task Emit(this Signal self)
    {

        var list = (Signal[])self.GetInvocationList();
        if (list == null)
        {
            return;
        }
        if (list.Length == 1)
        {
            await self(self);
        }

        for (int i = 0; i < list.Length; i++)
        {
            var cb = list[i];
            await cb(cb);
        }
    }
    public static async Task Emit<T1>(this Signal<T1> self, T1 arg1)
    {

        var list = (Signal<T1>[])self.GetInvocationList();
        if (list == null)
        {
            return;
        }
        if (list.Length == 1)
        {
            await self(arg1, self);
        }

        for (int i = 0; i < list.Length; i++)
        {
            var cb = list[i];
            await cb(arg1, cb);
        }
    }
    public static async Task Emit<T1, T2>(this Signal<T1, T2> self, T1 arg1, T2 arg2)
    {

        var list = (Signal<T1, T2>[])self.GetInvocationList();
        if (list == null)
        {
            return;
        }
        if (list.Length == 1)
        {
            await self(arg1, arg2, self);
        }

        for (int i = 0; i < list.Length; i++)
        {
            var cb = list[i];
            await cb(arg1, arg2, cb);
        }
    }
}


//public class Event<T1, T2>
//{
//    public event OnMessageHandler<T1, T2> OnMessage;

//    public void Listen(OnMessageHandler<T1, T2> cb) => OnMessage += cb;

//    public void Emit(T1 ipcMessage, T2 ipc)
//    {
//        if (OnMessage is null) return;
//        foreach (OnMessageHandler<T1, T2> cb in OnMessage.GetInvocationList().Cast<OnMessageHandler<T1, T2>>())
//        {
//            cb(ipcMessage, ipc);
//        }
//    }

//    public void Remove(OnMessageHandler<T1, T2> cb) => OnMessage -= cb;

//    public void Clear()
//    {
//        if (OnMessage is null) return;
//        foreach (OnMessageHandler<T1, T2> cb in OnMessage.GetInvocationList().Cast<OnMessageHandler<T1, T2>>())
//        {
//            Remove(cb);
//        }
//    }
//}

//public class SingleEvent<T>
//{
//    public event OnSingleMessageHandler<T> OnMessage;

//    public void Listen(OnSingleMessageHandler<T> cb) => OnMessage += cb;

//    public void Emit(T ipcMessage)
//    {
//        if (OnMessage is null) return;
//        foreach (OnSingleMessageHandler<T> cb in OnMessage.GetInvocationList().Cast<OnSingleMessageHandler<T>>())
//        {
//            cb(ipcMessage);
//        }
//    }

//    public void Remove(OnSingleMessageHandler<T> cb) => OnMessage -= cb;

//    public void Clear()
//    {
//        if (OnMessage is null) return;
//        foreach (OnSingleMessageHandler<T> cb in OnMessage.GetInvocationList().Cast<OnSingleMessageHandler<T>>())
//        {
//            Remove(cb);
//        }
//    }
//}

//public class SimpleEvent
//{
//    public event OnSimpleMessageHandler OnMessage;

//    public void Listen(OnSimpleMessageHandler cb) => OnMessage += cb;

//    public void Emit()
//    {
//        if (OnMessage is null) return;
//        foreach (OnSimpleMessageHandler cb in OnMessage.GetInvocationList().Cast<OnSimpleMessageHandler>())
//        {
//            cb();
//        }
//    }

//    public void Remove(OnSimpleMessageHandler cb) => OnMessage -= cb;

//    public void Clear()
//    {
//        if (OnMessage is null) return;
//        foreach (OnSimpleMessageHandler cb in OnMessage.GetInvocationList().Cast<OnSimpleMessageHandler>())
//        {
//            Remove(cb);
//        }
//    }
//}
