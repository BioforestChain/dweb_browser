using System;
using System.Collections.Generic;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace ipc
{
    [JsonConverter(typeof(IpcHeadersConverter))]
    public class IpcHeaders
	{
		private Dictionary<string, string> headersMap { get; set; }

		internal IpcHeaders()
		{ }

		public IpcHeaders(HttpHeaders headers)
		{
            headersMap = headers.ToDictionary(h => h.Key, h => h.Value.FirstOrDefault() ?? "");
        }

		public static IpcHeaders With(Dictionary<string, string> headers)
		{
			return new IpcHeaders() { headersMap = headers };
		}

		public void Set(string key, string value)
		{
			headersMap.Add(key.ToLower(), value);
		}

		public void Init(string key, string value)
		{
			if (!headersMap.ContainsKey(key))
			{
				headersMap.Add(key.ToLower(), value);
			}
		}

		public string? Get(string key)
		{
			return headersMap.GetValueOrDefault(key.ToLower());
		}

		public string GetOrDefault(string key, string defaultValue)
		{
			return headersMap.GetValueOrDefault(key.ToLower()) ?? defaultValue;
		}

		public bool Has(string key)
		{
			return headersMap.ContainsKey(key.ToLower());
		}

		public void Delete(string key)
		{
			headersMap.Remove(key.ToLower());
		}

		// TODO: IpcHeaders forEach 未实现
		//public void forEach(Func<string, string, Action<Tuple>> fn)
		//{

		//}

		public List<KeyValuePair<string, string>> ToList()
		{
			var list = new List<KeyValuePair<string, string>>();

            foreach (KeyValuePair<string, string> entry in headersMap)
            {
				list.Add(entry);
			}

			return list;
		}

		public Dictionary<string, string> ToMap()
		{
			return headersMap;
		}

        /// <summary>
        /// Serialize IpcHeaders
        /// </summary>
        /// <returns>JSON string representation of the IpcHeaders</returns>
        public string ToJson()
		{
            return JsonSerializer.Serialize(this);
        }

        /// <summary>
        /// Deserialize IpcHeaders
        /// </summary>
        /// <param name="json">JSON string representation of IpcHeaders</param>
        /// <returns>An instance of a IpcHeaders object.</returns>
        public static IpcHeaders? FromJson(string json)
		{
            return JsonSerializer.Deserialize<IpcHeaders>(json);
        }
	}

	sealed class IpcHeadersConverter : JsonConverter<IpcHeaders>
	{
		public override bool CanConvert(Type typeToConvert) =>
			typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;

        public override IpcHeaders? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            if (reader.TokenType != JsonTokenType.StartObject)
                throw new JsonException("Expected StartObject token");

			var headers = new Dictionary<string, string>();

			while (reader.Read())
			{
				if (reader.TokenType == JsonTokenType.EndObject)
				{
					return IpcHeaders.With(headers);
                }
                    

                if (reader.TokenType != JsonTokenType.PropertyName)
                    throw new JsonException("Expected PropertyName token");

				var key = reader.GetString();

				reader.Read();

				if (key != "")
				{
                    headers.Add(key!, reader.GetString() ?? "");
                }
            }

            throw new JsonException("Expected EndObject token");
        }

        public override void Write(Utf8JsonWriter writer, IpcHeaders value, JsonSerializerOptions options)
        {
			writer.WriteStartObject();

            foreach (KeyValuePair<string, string> entry in value.ToMap())
            {
				Console.WriteLine($"key: {entry.Key} value: {entry.Value}");
				writer.WriteString(entry.Key, entry.Value);
            }

            writer.WriteEndObject();
        }
    }
}

