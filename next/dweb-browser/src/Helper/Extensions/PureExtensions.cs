#nullable enable
using System;
using System.Diagnostics.CodeAnalysis;
using System.Security.Claims;
using System.Text;
using System.Threading;
using DwebBrowser.MicroService.Http;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Http.Authentication;
using Microsoft.AspNetCore.Http.Features;
using Microsoft.AspNetCore.Http.Internal;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.Extensions.Primitives;
using Microsoft.Maui.Controls.Compatibility;
using Microsoft.Net.Http.Headers;

namespace DwebBrowser.Helper;

/// <summary>
///
/// 参考 https://github.com/dotnet/aspnetcore/blob/8562fe78898e4563c547f03b55eef518c183e812/src/Http/Http/src/Features/FormFeature.cs
/// </summary>
public static class PureExtensions
{

    static public async Task<FormCollection> ReadFromDataAsync(this PureRequest pureRequest, CancellationToken? _cancellationToken = default)
    {
        FormCollection? formFields = null;
        FormFileCollection? files = null;
        var form = FormCollection.Empty;

        if (MediaTypeHeaderValue.TryParse(pureRequest.Headers.Get("Content-Type"), out var contentType))
        {
            var cancellationToken = _cancellationToken ?? CancellationToken.None;
            var valueCountLimit = FormReader.DefaultValueCountLimit;
            var baseStream = pureRequest.Body.ToStream();

            // Check the content-type
            if (HasApplicationFormContentType(contentType))
            {
                var encoding = FilterEncoding(contentType.Encoding);
                /// TODO 为什么不能用  FormPipeReader？？
                var formReader = new FormReader(baseStream, encoding)
                {
                    ValueCountLimit = valueCountLimit,
                    KeyLengthLimit = FormReader.DefaultKeyLengthLimit,
                    ValueLengthLimit = FormReader.DefaultValueLengthLimit,
                };
                formFields = new FormCollection(await formReader.ReadFormAsync(cancellationToken));
            }
            else if (HasMultipartFormContentType(contentType))
            {
                var formAccumulator = new KeyValueAccumulator();
                var sectionCount = 0;

                var boundary = GetBoundary(contentType, DefaultMultipartBoundaryLengthLimit);
                var multipartReader = new MultipartReader(boundary, baseStream)
                {
                    HeadersCountLimit = MultipartReader.DefaultHeadersCountLimit,
                    HeadersLengthLimit = MultipartReader.DefaultHeadersLengthLimit,
                    BodyLengthLimit = DefaultMultipartBodyLengthLimit,
                };
                var section = await multipartReader.ReadNextSectionAsync(cancellationToken);
                while (section != null)
                {
                    sectionCount++;
                    if (sectionCount > valueCountLimit)
                    {
                        throw new InvalidDataException($"Form value count limit {valueCountLimit} exceeded.");
                    }
                    // Parse the content disposition here and pass it further to avoid reparsings
                    if (!ContentDispositionHeaderValue.TryParse(section.ContentDisposition, out var contentDisposition))
                    {
                        throw new InvalidDataException("Form section has invalid Content-Disposition value: " + section.ContentDisposition);
                    }

                    if (contentDisposition.IsFileDisposition())
                    {
                        var fileSection = new FileMultipartSection(section, contentDisposition);


                        // Find the end
                        await section.Body.DrainAsync(cancellationToken);

                        var name = fileSection.Name;
                        var fileName = fileSection.FileName;

                        FormFile file;
                        if (section.BaseStreamOffset.HasValue)
                        {
                            // Relative reference to buffered request body
                            file = new FormFile(baseStream, section.BaseStreamOffset.GetValueOrDefault(), section.Body.Length, name, fileName);
                        }
                        else
                        {
                            // Individually buffered file body
                            file = new FormFile(section.Body, 0, section.Body.Length, name, fileName);
                        }
                        file.Headers = new HeaderDictionary(section.Headers);

                        if (files == null)
                        {
                            files = new FormFileCollection();
                        }
                        files.Add(file);
                    }
                    else if (contentDisposition.IsFormDisposition())
                    {
                        var formDataSection = new FormMultipartSection(section, contentDisposition);

                        // Content-Disposition: form-data; name="key"
                        //
                        // value

                        // Do not limit the key name length here because the multipart headers length limit is already in effect.
                        var key = formDataSection.Name;
                        var value = await formDataSection.GetValueAsync();

                        formAccumulator.Append(key, value);
                    }
                    else
                    {
                        // Ignore form sections with invalid content disposition
                    }

                    section = await multipartReader.ReadNextSectionAsync(cancellationToken);
                }

                if (formAccumulator.HasValues)
                {
                    formFields = new FormCollection(formAccumulator.GetResults(), files);
                }
            }
        }
        if (formFields != null)
        {
            form = formFields;
        }
        else if (files != null)
        {
            form = new FormCollection(null, files);
        }

        return form;
    }

    /// <summary>
    /// Default value for <see cref="MemoryBufferThreshold"/>.
    /// Defaults to 65,536 bytes, which is approximately 64KB.
    /// </summary>
    public const int DefaultMemoryBufferThreshold = 1024 * 64;

    /// <summary>
    /// Default value for <see cref="BufferBodyLengthLimit"/>.
    /// Defaults to 134,217,728 bytes, which is 128MB.
    /// </summary>
    public const int DefaultBufferBodyLengthLimit = 1024 * 1024 * 128;

    /// <summary>
    /// Default value for <see cref="MultipartBoundaryLengthLimit"/>.
    /// Defaults to 128 bytes.
    /// </summary>
    public const int DefaultMultipartBoundaryLengthLimit = 128;

    /// <summary>
    /// Default value for <see cref="MultipartBodyLengthLimit "/>.
    /// Defaults to 134,217,728 bytes, which is approximately 128MB.
    /// </summary>
    public const long DefaultMultipartBodyLengthLimit = 1024 * 1024 * 128;


    private static Encoding FilterEncoding(Encoding? encoding)
    {
        // UTF-7 is insecure and should not be honored. UTF-8 will succeed for most cases.
        // https://learn.microsoft.com/en-us/dotnet/core/compatibility/syslib-warnings/syslib0001
        if (encoding == null || encoding.CodePage == 65000)
        {
            return Encoding.UTF8;
        }
        return encoding;
    }
    private static bool HasApplicationFormContentType([NotNullWhen(true)] MediaTypeHeaderValue? contentType)
    {
        // Content-Type: application/x-www-form-urlencoded; charset=utf-8
        return contentType != null && contentType.MediaType.Equals("application/x-www-form-urlencoded", StringComparison.OrdinalIgnoreCase);
    }
    private static bool HasMultipartFormContentType([NotNullWhen(true)] MediaTypeHeaderValue? contentType)
    {
        // Content-Type: multipart/form-data; boundary=----WebKitFormBoundarymx2fSWqWSd0OxQqq
        return contentType != null && contentType.MediaType.Equals("multipart/form-data", StringComparison.OrdinalIgnoreCase);
    }
    private static string GetBoundary(MediaTypeHeaderValue contentType, int lengthLimit)
    {
        var boundary = HeaderUtilities.RemoveQuotes(contentType.Boundary);
        if (StringSegment.IsNullOrEmpty(boundary))
        {
            throw new InvalidDataException("Missing content-type boundary.");
        }
        if (boundary.Length > lengthLimit)
        {
            throw new InvalidDataException($"Multipart boundary length limit {lengthLimit} exceeded.");
        }
        return boundary.ToString();
    }
}
