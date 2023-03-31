
namespace micro_service.helper;

public delegate Task? Signal(Signal self);
public delegate Task? Signal<T1>(T1 arg1, Signal<T1> self);
public delegate Task? Signal<T1, T2>(T1 arg1, T2 arg2, Signal<T1, T2> self);



public static class SignalExtendsions
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
            await self(self).ForAwait();
        }

        for (int i = 0; i < list.Length; i++)
        {
            var cb = list[i];
            await cb(cb).ForAwait();
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
            await self(arg1, self).ForAwait();
        }

        for (int i = 0; i < list.Length; i++)
        {
            var cb = list[i];
            await cb(arg1, cb).ForAwait();
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
            await self(arg1, arg2, self).ForAwait();
        }

        for (int i = 0; i < list.Length; i++)
        {
            var cb = list[i];
            await cb(arg1, arg2, cb).ForAwait();
        }
    }
}

