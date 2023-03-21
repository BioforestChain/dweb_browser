using System;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace ipc
{
	public class IpcResponse
	{
        public IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.RESPONSE;

        internal IpcResponse()
		{
		}
	}

    [JsonConverter(typeof(IpcResMessageConverter))]
	public class IpcResMessage : IpcMessage
	{
        public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.RESPONSE;

		public int ReqId { get; set; }
		public int StatusCode { get; set; }
		public Dictionary<string, string> Headers { get; set; }

        public IpcResMessage(int req_id, int statusCode, Dictionary<string, string> headers)
        {
            ReqId = req_id;
            StatusCode = statusCode;
            Headers = headers;
        }

        internal IpcResMessage()
        { }


        /// <summary>
        /// Serialize IpcReqMessage
        /// </summary>
        /// <returns>JSON string representation of the IpcReqMessage</returns>
        public string ToJson()
        {
            return JsonSerializer.Serialize(this);
        }

        /// <summary>
        /// Deserialize IpcReqMessage
        /// </summary>
        /// <param name="json">JSON string representation of IpcReqMessage</param>
        /// <returns>An instance of a IpcReqMessage object.</returns>
        public static IpcReqMessage? FromJson(string json)
        {
            return JsonSerializer.Deserialize<IpcReqMessage>(json);
        }
    }

    sealed class IpcResMessageConverter : JsonConverter<IpcResMessage>
    {
        public override bool CanConvert(Type typeToConvert) =>
            typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


        public override IpcResMessage? Read(
            ref Utf8JsonReader reader,
            Type typeToConvert,
            JsonSerializerOptions options)
        {
            if (reader.TokenType != JsonTokenType.StartObject)
                throw new JsonException("Expected StartObject token");

            var ipcResMessage = new IpcResMessage();
            while (reader.Read())
            {
                if (reader.TokenType == JsonTokenType.EndObject)
                    return ipcResMessage;

                if (reader.TokenType != JsonTokenType.PropertyName)
                    throw new JsonException("Expected PropertyName token");

                var propName = reader.GetString();

                reader.Read();

                switch (propName)
                {
                    case "req_id":
                        ipcResMessage.ReqId = reader.GetInt32();
                        break;
                    case "type":
                        ipcResMessage.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                        break;
                    case "statusCode":
                        ipcResMessage.StatusCode = reader.GetInt16();
                        break;
                    case "headers":
                        var headers = new Dictionary<string, string>();

                        while (reader.Read())
                        {
                            if (reader.TokenType == JsonTokenType.StartObject)
                            {
                                continue;
                            }

                            if (reader.TokenType == JsonTokenType.EndObject)
                            {
                                ipcResMessage.Headers = headers;
                                break;
                            }

                            var memberName = reader.GetString();

                            reader.Read();

                            if (memberName != null)
                            {
                                headers.Add(memberName, reader.GetString() ?? "");
                            }
                        }


                        break;
                }
            }

            throw new JsonException("Expected EndObject token");
        }

        public override void Write(
            Utf8JsonWriter writer,
            IpcResMessage value,
            JsonSerializerOptions options)
        {
            writer.WriteStartObject();

            writer.WriteNumber("req_id", value.ReqId);
            writer.WriteNumber("type", (int)value.Type);
            writer.WriteNumber("statusCode", value.StatusCode);

            // dictionary
            writer.WritePropertyName("headers");
            writer.WriteStartObject();

            foreach ((string key, string keyValue) in value.Headers)
            {

                writer.WriteString(key, keyValue);
            }

            writer.WriteEndObject();

            writer.WriteEndObject();
        }
    }
}

