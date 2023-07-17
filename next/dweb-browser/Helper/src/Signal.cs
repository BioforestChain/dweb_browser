namespace DwebBrowser.Helper;

public delegate Task? Signal(Signal self);
public delegate Task? Signal<T1>(T1 arg1, Signal<T1> self);
public delegate Task? Signal<T1, T2>(T1 arg1, T2 arg2, Signal<T1, T2> self);

public class SignalResult<R>
{
    public R? Result;
    public bool HasResult = false;
    /// <summary>
    /// 写入结果
    /// </summary>
    /// <param name="result"></param>
    public void Complete(R result)
    {
        if (HasResult) return;
        Result = result;
        HasResult = true;
        Next();
    }
    /// <summary>
    /// 跳过处置，由下一个处理者接管
    /// </summary>
    /// <param name="nexter"></param>
    public void Next() => Waitter.Resolve(Unit.Default);
    internal PromiseOut<Unit> Waitter = new();
}


public static class SignalExtendsions
{
    static readonly Debugger Console = new("Signal");
    public static bool IsEmpty(this Signal self)
    {
        return self.GetInvocationList().Length is 0;
    }
    public static async Task Emit(this Signal? self)
    {
        try
        {
            var list = self?.GetInvocationList();
            if (list is null) return;

            if (list.Length == 1)
            {
                await self!(self).ForAwait();
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
    public static async Task Emit<T1>(this Signal<T1>? self, T1 arg1)
    {

        try
        {
            var list = self?.GetInvocationList();
            if (list is null) return;

            if (list.Length == 1)
            {
                await self!(arg1, self).ForAwait();
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
    public static async Task Emit<T1, T2>(this Signal<T1, T2>? self, T1 arg1, T2 arg2)
    {

        try
        {
            var list = self?.GetInvocationList();
            if (list is null) return;

            if (list.Length == 1)
            {
                await self!(arg1, arg2, self).ForAwait();
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

    public static async Task<(R?,bool)> EmitForResult<T, R>(this Signal<T, SignalResult<R>>? self, T args, Signal<T, SignalResult<R>> finallyNext)
    {
        try
        {
            Delegate[] list = (self?.GetInvocationList() ?? Array.Empty<Delegate>()).Append(finallyNext).ToArray();

            for (int i = 0; i < list.Length; i++)
            {
                var cb = (Signal<T, SignalResult<R>>)list[i];

                var ctx = new SignalResult<R>();
                await cb(args, ctx, cb).ForAwait();
                await ctx.Waitter.WaitPromiseAsync();
                if (ctx.HasResult)
                {
                    return (ctx.Result,true);
                }
            }
        }
        catch (Exception e)
        {
            Console.Error("EmitForResult", "{0}", e);
        }
        return (default(R),false);

    }

    public static async Task EmitAndClear(this Signal? self)
    {
        await (self?.Emit()).ForAwait();
        self = null;
    }

    public static async Task EmitAndClear<T1>(this Signal<T1>? self, T1 arg1)
    {
        await (self?.Emit(arg1)).ForAwait();
        self = null;
    }

    public static async Task EmitAndClear<T1, T2>(this Signal<T1, T2>? self, T1 arg1, T2 arg2)
    {
        await (self?.Emit(arg1, arg2)).ForAwait();
        self = null;
    }
}

