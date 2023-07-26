using System.Text.Json;
using DwebBrowser.MicroService.Core;

namespace DwebBrowser.MicroServiceTests.Core;

public class MicroModuleCategoryTest
{
	[Fact]
	public void JsonSerializerMicroModuleCategoryTest()
	{
		var categories = new List<MicroModuleCategory> { MicroModuleCategory.Application, MicroModuleCategory.Books };

		var json = JsonSerializer.Serialize(categories);
		Debug.WriteLine(json);
		var fromJson = JsonSerializer.Deserialize<List<MicroModuleCategory>>(json);
		Debug.WriteLine(fromJson);
		if (fromJson is not null)
		{
            foreach (var category in fromJson)
            {
				Debug.WriteLine(category);
            }
        }

		Assert.Equal(categories, fromJson);
	}
}

