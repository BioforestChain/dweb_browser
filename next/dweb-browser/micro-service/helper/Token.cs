
using System.Security.Cryptography;

namespace micro_service;

public static class Token
{
    public static string Base64UrlEncode(byte[] input)
    {
        string base64 = Convert.ToBase64String(input);
        base64 = base64.Replace("+", "-").Replace("/", "_").TrimEnd('=');

        return base64;
    }

    public static string RandomCryptoString(int n)
    {
        var byteArray = new byte[n];

        using (var rng = RandomNumberGenerator.Create())
        {
            rng.GetBytes(byteArray);
        }

        return Base64UrlEncode(byteArray);
    }
}