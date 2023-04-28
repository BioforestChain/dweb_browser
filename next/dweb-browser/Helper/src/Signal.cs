namespace DwebBrowser.Helper;

public delegate Task? Signal(Signal self);
public delegate Task? Signal<T1>(T1 arg1, Signal<T1> self);
public delegate Task? Signal<T1, T2>(T1 arg1, T2 arg2, Signal<T1, T2> self);



public static class SignalExtendsions
{
    static Debugger Console = new("Signal");
    public static bool IsEmpty(this Signal self)
    {
        return self.GetInvocationList().Length is 0;
    }
    public static async Task Emit(this Signal self)
    {
        try
        {
            var list = self.GetInvocationList();
            if (list == null)
            {
                return;
            }
            if (list.Length == 1)
            {
                await self(self).ForAwait();
                return;
            }

            for (int i = 0; i < list.Length; i++)
            {
                var cb = (Signal)list[i];
                await cb(cb).ForAwait();
            }
        }
        catch (Exception e)
        {
            Console.Error("Emit", "{0}", e);
        }
    }
    public static async Task Emit<T1>(this Signal<T1> self, T1 arg1)
    {

        try
        {
            var list = self.GetInvocationList();
            if (list == null)
            {
                return;
            }
            if (list.Length == 1)
            {
                await self(arg1, self).ForAwait();
                return;
            }

            for (int i = 0; i < list.Length; i++)
            {
                var cb = (Signal<T1>)list[i];
                await cb(arg1, cb).ForAwait();
            }
        }
        catch (Exception e)
        {
            Console.Error("Emit", "{0}", e);
        }
    }
    public static async Task Emit<T1, T2>(this Signal<T1, T2> self, T1 arg1, T2 arg2)
    {

        try
        {
            var list = self.GetInvocationList();
            if (list == null)
            {
                return;
            }
            if (list.Length == 1)
            {
                await self(arg1, arg2, self).ForAwait();
                return;
            }

            for (int i = 0; i < list.Length; i++)
            {
                var cb = (Signal<T1, T2>)list[i];
                await cb(arg1, arg2, cb).ForAwait();
            }
        }
        catch (Exception e)
        {
            Console.Error("Emit", "{0}", e);
        }
    }
}

