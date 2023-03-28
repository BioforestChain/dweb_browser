
using System.Text;
using System.Text.Json;
using Xunit.Abstractions;

namespace ipc_test.ipc;

public class IpcMessageSerializeTest : Log
{
	public IpcMessageSerializeTest(ITestOutputHelper output) : base(output)
	{
	}

	[Fact]
	[Trait("Ipc", "IpcMessage")]
	public void ToJson_IpcStreamAbort_ReturnsSerializeJson()
	{
		Assert.Equal(@"{""type"":6,""stream_id"":""stream_id""}", new IpcStreamAbort("stream_id").ToJson());
	}

	//[Fact]
	//public void FromJson_IpcStreamAbort_ReturnsIpcStreamAbort()
	//{
		//Console.WriteLine(IpcStreamAbort.FromJson(@"{""type"":6,""stream_id"":""stream_id""}"));
		//Console.WriteLine(new IpcStreamAbort("stream_id").ToJson());
		//Console.WriteLine(IpcStreamAbort.FromJson(new IpcStreamAbort("stream_id").ToJson()));
		//Console.WriteLine(JsonSerializer.Deserialize<IpcMessageType>(@"{""type"":6,""stream_id"":""stream_id""}"));
	//}

	[Fact]
	[Trait("Ipc", "IpcMessage")]
	public void MessagePackFromJson_IpcStreamAbort_ReturnsByteArray()
	{
		Assert.Equal(
			new byte[] {
				130, 164, 116, 121, 112, 101, 6, 169, 115, 116, 114,
				101, 97, 109, 95, 105, 100, 169, 115, 116, 114, 101,
				97, 109, 95, 105, 100 },
			MessagePack.MessagePackSerializer.ConvertFromJson(@"{""type"":6,""stream_id"":""stream_id""}"));
	}

	[Fact]
	public void MessagePackToJson_IpcStreamAbort_ReturnsJson()
	{
		//Console.WriteLine(MessagePack.MessagePackSerializer.ConvertToJson(new byte[] {
  //              130, 164, 116, 121, 112, 101, 6, 169, 115, 116, 114,
  //              101, 97, 109, 95, 105, 100, 169, 115, 116, 114, 101,
  //              97, 109, 95, 105, 100 }));

		Assert.Equal(@"{""type"":6,""stream_id"":""stream_id""}", MessagePack.MessagePackSerializer.ConvertToJson(new byte[] {
                130, 164, 116, 121, 112, 101, 6, 169, 115, 116, 114,
                101, 97, 109, 95, 105, 100, 169, 115, 116, 114, 101,
                97, 109, 95, 105, 100 }));

    }
}

