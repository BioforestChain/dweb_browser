
namespace DwebBrowser.HelperTests;


public class UnitTest
{
	[Fact]
	public Unit UnitTypeTest()
	{
		Assert.IsType<Unit>(Unit.Default);
		return Unit.Default;
	}

	[Fact]
	public async Task<Unit> PromiseOutUnitTypeTest_Returns()
	{
		var po = new PromiseOut<Unit>();

		_ = Task.Run(async () =>
		{
			await Task.Delay(100);
			po.Resolve(Unit.Default);
		});

		var res = await po.WaitPromiseAsync();

		Assert.Equal(Unit.Default, res);

        return res;
	}

	public struct UnitType
	{
		public static readonly UnitType unit = new UnitType();

		public UnitType() { }
	}
}

