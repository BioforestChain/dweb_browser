using System.Security.Cryptography;
using System.Text.Json;
using DwebBrowser.MicroService.Sys.Device;
using Foundation;

#nullable enable

namespace DwebBrowser.Helper;

public struct StoreOptions
{
    public record SecretOptions(byte[] key, byte[] iv);

    public SecretOptions? Secret;
}

/// <summary>
/// 使用iOS NSUserDefaults进行持久化
/// </summary>
sealed public class Store
{
    static readonly Debugger Console = new("Store");
    private byte[] SecretKey { get; init; }
    private byte[] SecretIv { get; init; }

    private string StoreName { get; init; }
    private static readonly NSUserDefaults _userDefaults = NSUserDefaults.StandardUserDefaults;

    public Store(string name, StoreOptions options = new())
    {
        StoreName = name;

        if (_userDefaults.DictionaryForKey(StoreName) is null)
        {
            var nsDic = new NSMutableDictionary<NSString, NSObject>();
            _userDefaults.SetValueForKey(nsDic, new NSString(StoreName));
        }

        if (options.Secret is not null)
        {
            SecretKey = options.Secret.key;
            SecretIv = options.Secret.iv;
        }
        else
        {
            var key = CborHelper.Encode(StoreName);
            var iv = SHA256.HashData(CborHelper.Encode(JsonSerializer.Serialize(DeviceSystemInfo.GetDeviceInfo())));

            using var hmac = new HMACSHA256(key);
            SecretKey = hmac.ComputeHash(iv);

            // 截取前16字节作为iv
            SecretIv = new byte[16];
            Array.Copy(iv, SecretIv, 16);
        }
    }

    /// <summary>
    /// aes-256-cbc encode
    /// </summary>
    /// <param name="value"></param>
    /// <returns></returns>
    private string Encode(string value)
    {
        byte[] encrypted;
        using (Aes aesAlg = Aes.Create())
        {
            aesAlg.KeySize = 256;
            aesAlg.Mode = CipherMode.CBC;
            aesAlg.Key = SecretKey;
            aesAlg.IV = SecretIv;

            ICryptoTransform encryptor = aesAlg.CreateEncryptor(aesAlg.Key, aesAlg.IV);

            using MemoryStream msEncrypt = new();
            using CryptoStream csEncrypt = new(msEncrypt, encryptor, CryptoStreamMode.Write);
            using (StreamWriter swEncrypt = new(csEncrypt))
            {
                swEncrypt.Write(value);
            }
            encrypted = msEncrypt.ToArray();
        }

        return encrypted.ToBase64();
    }

    /// <summary>
    /// aes-256-cbc decoded
    /// </summary>
    /// <param name="encoded"></param>
    /// <returns></returns>
    private string Decode(string encoded)
    {
        var cipherText = encoded.ToBase64ByteArray();

        var plainText = string.Empty;
        using (Aes aesAlg = Aes.Create())
        {
            aesAlg.Mode = CipherMode.CBC;
            aesAlg.KeySize = 256;
            aesAlg.Key = SecretKey;
            aesAlg.IV = SecretIv;

            var decryptor = aesAlg.CreateDecryptor(aesAlg.Key, aesAlg.IV);
            using MemoryStream msDecrypt = new(cipherText);
            using CryptoStream csDecrypt = new(msDecrypt, decryptor, CryptoStreamMode.Read);
            using StreamReader srDecrypt = new(csDecrypt);

            plainText = srDecrypt.ReadToEnd();
        }

        return plainText;
    }

    public string? Get(string key, Func<string>? orDefault)
    {
        var nsDic = _userDefaults.DictionaryForKey(StoreName);

        if (nsDic!.TryGetValue(new NSString(key), out var res))
        {
            return Decode(res.ToString());
        }

        if (orDefault is not null)
        {
            var value = orDefault();
            if (Set(key, value))
            {
                return value;
            }
        }

        return null;
    }

    public bool Set(string key, string value)
    {
        try
        {
            var nsDic = _userDefaults.DictionaryForKey(StoreName);
            var mutDic = new NSMutableDictionary<NSString, NSString>();

            _ = nsDic ?? throw new KeyNotFoundException($"{StoreName} not found");

            foreach (var (k, v) in nsDic)
            {
                mutDic.Add(k, v);
            }

            mutDic.Add(new NSString(key), new NSString(Encode(value)));

            _userDefaults.SetValueForKey(mutDic, new NSString(StoreName));

            return true;
        }
        catch
        {
            return false;
        }
    }

    public bool Delete(string key)
    {
        try
        {
            var nsDic = _userDefaults.DictionaryForKey(StoreName);
            var mutDic = new NSMutableDictionary<NSString, NSString>();

            foreach (var (k, v) in nsDic!)
            {
                if (k.ToString() != key)
                {
                    mutDic.Add(k, v);
                }
            }

            _userDefaults.SetValueForKey(mutDic, new NSString(StoreName));

            return true;
        }
        catch
        {
            return false;
        }
    }

    public void Clear()
    {
        _userDefaults.RemoveObject(StoreName);
    }
}

/// <summary>
/// 使用文件进行持久化存储
/// </summary>
public class FileStore
{
    static readonly Debugger Console = new("FileStore");

    private byte[] SecretKey { get; init; }
    private byte[] SecretIv { get; init; }

    private string StoreName { get; init; }

    internal FileStore(string name, StoreOptions options = default)
    {
        StoreName = name;

        if (options.Secret is not null)
        {
            SecretKey = options.Secret.key;
            SecretIv = options.Secret.iv;
        }
        else
        {
            var key = CborHelper.Encode(StoreName);
            var iv = SHA256.HashData(CborHelper.Encode(JsonSerializer.Serialize(DeviceSystemInfo.GetDeviceInfo())));

            using var hmac = new HMACSHA256(key);
            SecretKey = hmac.ComputeHash(iv);

            // 截取前16字节作为iv
            SecretIv = new byte[16];
            Array.Copy(iv, SecretIv, 16);
        }
    }

    internal static readonly string DATA_DIR = Path.Join(PathHelper.GetIOSDocumentDirectory(), "datastore");

    /// <summary>
    /// aes-256-cbc encode
    /// </summary>
    /// <param name="value"></param>
    /// <returns></returns>
    private byte[] Encode(string value)
    {
        byte[] encrypted;
        using (Aes aesAlg = Aes.Create())
        {
            aesAlg.KeySize = 256;
            aesAlg.Mode = CipherMode.CBC;
            aesAlg.Key = SecretKey;
            aesAlg.IV = SecretIv;

            ICryptoTransform encryptor = aesAlg.CreateEncryptor(aesAlg.Key, aesAlg.IV);

            using MemoryStream msEncrypt = new();
            using CryptoStream csEncrypt = new(msEncrypt, encryptor, CryptoStreamMode.Write);
            using (StreamWriter swEncrypt = new(csEncrypt))
            {
                swEncrypt.Write(value);
            }
            encrypted = msEncrypt.ToArray();
        }

        return encrypted;
    }

    /// <summary>
    /// aes-256-cbc decoded
    /// </summary>
    /// <param name="encoded"></param>
    /// <returns></returns>
    private string Decode(string encoded)
    {
        var bytes = encoded.ToBase64ByteArray();

        return Decode(bytes);
    }

    private string Decode(byte[] bytes)
    {
        var plainText = string.Empty;
        using (Aes aesAlg = Aes.Create())
        {
            aesAlg.Mode = CipherMode.CBC;
            aesAlg.KeySize = 256;
            aesAlg.Key = SecretKey;
            aesAlg.IV = SecretIv;

            var decryptor = aesAlg.CreateDecryptor(aesAlg.Key, aesAlg.IV);
            using MemoryStream msDecrypt = new(bytes);
            using CryptoStream csDecrypt = new(msDecrypt, decryptor, CryptoStreamMode.Read);
            using StreamReader srDecrypt = new(csDecrypt);

            plainText = srDecrypt.ReadToEnd();
        }

        return plainText;
    }

    private string Decode(Stream stream)
    {
        var binaryReader = stream.GetBinaryReader();
        var bytes = binaryReader.ReadBytes(stream.Length.ToInt());

        return Decode(bytes);
    }

    private static string ResolveKey(string key, bool autoCreate = true)
    {
        var filePath = Path.Join(DATA_DIR, $"{key}.cbor");

        if (autoCreate && !File.Exists(filePath) && Path.GetDirectoryName(filePath) is var fileDir && fileDir is not null)
        {
            Directory.CreateDirectory(fileDir);
            File.WriteAllBytes(filePath, Array.Empty<byte>());
        }

        return filePath;
    }

    /// <summary>
    /// 取出值，取出值之后需要再反序列化为目标值
    /// </summary>
    /// <param name="key"></param>
    /// <param name="orDefault"></param>
    /// <returns></returns>
    public async Task<string?> GetAsync(string key, Func<string>? orDefault = null)
    {
        var stream = File.OpenRead(ResolveKey(key));
        try
        {
            var decoded = Decode(stream);

            if (string.IsNullOrWhiteSpace(decoded))
            {
                throw new Exception("The first call needs to be initialized with default.");
            }

            return decoded;
        }
        catch
        {
            if (orDefault is not null)
            {
                var defaultValue = orDefault();
                if (await SetAsync(key, defaultValue))
                {
                    return defaultValue;
                }

                throw new Exception($"fail to save store for {StoreName}");
            }

            return null;
        }
        finally
        {
            stream.Dispose();
        }
    }

    public string? Get(string key, Func<string>? orDefault = null)
    {
        var isClosed = false;
        var stream = File.OpenRead(ResolveKey(key));
        try
        {
            var decoded = Decode(stream);

            if (string.IsNullOrWhiteSpace(decoded))
            {
                isClosed = true;
                stream.Dispose();
                throw new Exception("The first call needs to be initialized with default.");
            }

            return decoded;
        }
        catch
        {
            if (orDefault is not null)
            {
                var defaultValue = orDefault();
                if (Set(key, defaultValue))
                {
                    return defaultValue;
                }

                throw new Exception($"fail to save store for {StoreName}");
            }

            return null;
        }
        finally
        {
            if (!isClosed)
            {
                stream.Dispose();
            }
        }
    }

    /// <summary>
    /// 设置值，需要先序列化之后传值进来存储
    /// </summary>
    /// <param name="key"></param>
    /// <param name="value"></param>
    public async Task<bool> SetAsync(string key, string value)
    {
        try
        {
            using var stream = File.OpenWrite(ResolveKey(key));
            stream.Write(Encode(value));
            return true;
        }
        catch
        {
            return false;
        }
    }

    public bool Set(string key, string value)
    {
        try
        {
            using var stream = File.OpenWrite(ResolveKey(key));
            stream.Write(Encode(value));
            return true;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// 删除key
    /// </summary>
    /// <param name="key"></param>
    /// <returns></returns>
    public bool Delete(string key)
    {
        try
        {
            File.Delete(ResolveKey(key, false));
            return true;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// 清空
    /// </summary>
    public void Clear()
    {
        Directory.Delete(DATA_DIR, true);
    }
}
