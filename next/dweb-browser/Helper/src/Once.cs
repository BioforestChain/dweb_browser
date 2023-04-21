
namespace DwebBrowser.Helper;

public static class Once
{
	public static Func<Task<R>> AsyncOnce<R>(Func<Task<R>> runnalble)
	{
		var runned = false;
		object? result = null;

		return async () =>
		{
			if (!runned)
			{
				runned = true;
				result = await runnalble();
			}

			return (R)Convert.ChangeType(result, typeof(R));
		};
	}
}

