using System;

namespace micro_service.core;

public abstract class NativeMicroModule: MicroModule
{
	static NativeMicroModule()
	{
        NativeConnect.ConnectAdapterManager.Append((fromMM, toMM, reason) =>
        {
            if (toMM is NativeMicroModule nmm)
            {
                var channel = new NativeMessageChannel<IpcMessage, IpcMessage>();
                var toNativeIpc = new NativeIpc(channel.Port1, fromMM, IPC_ROLE.SERVER);
                var fromNativeIpc = new NativeIpc(channel.Port2, nmm, IPC_ROLE.CLIENT);
                fromMM.BeConnect(fromNativeIpc, reason);
                nmm.BeConnect(toNativeIpc, reason);
                return new ConnectResult(fromNativeIpc, toNativeIpc);
            }

            return null;
        });

    }

    public class ResponseRegistry
    {
        public static Dictionary<object, Func<object, HttpResponseMessage>> RegMap = new();

        public static void RegistryResponse<T>(object type, Func<T, HttpResponseMessage> handler) =>
            RegMap.TryAdd(type, handler as Func<object, HttpResponseMessage>);

        //public static HttpResponseMessage Handler(object result)
        //{

        //}
    }
}
