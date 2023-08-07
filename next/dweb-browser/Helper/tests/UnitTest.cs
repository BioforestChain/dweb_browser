
namespace DwebBrowser.HelperTests;


public class UnitTest
{
	[Fact]
	public void UnitTypeTest()
	{
		Assert.IsType<Unit>(Unit.Default);
	}

	[Fact]
	public async Task PromiseOutUnitTypeTest_Returns()
	{
		var po = new PromiseOut<Unit>();

		_ = Task.Run(async () =>
		{
			await Task.Delay(100);
			po.Resolve(Unit.Default);
		});

		var res = await po.WaitPromiseAsync();

		Assert.Equal(Unit.Default, res);
	}

	public struct UnitType
	{
		public static readonly UnitType unit = new UnitType();

		public UnitType() { }
	}
}

