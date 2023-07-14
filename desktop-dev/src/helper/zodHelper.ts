//! use zod error: Relative import path "zod" not prefixed with / or ./ or ../ only remote
//! https://github.com/denoland/deno/issues/17598
import { RefinementCtx, z } from "zod";
import { $MMID } from "../core/types.ts";
// const z_mmid = z.string().endsWith(".dweb");
export const mmidType = z.custom<$MMID>((val) => {
  return typeof val === "string" && val.endsWith(".dweb");
});
export * from "zod";
export { mmidType as mmid };

import type { output, SafeParseReturnType, ZodObject, ZodRawShape, ZodTypeAny } from "zod";

/// 以下代码来自 https://github.com/rileytomasek/zodix （MIT license）
/// 因为源码有点问题，并且引入了一些不该引入的包，所以这里只提取了部分有需要的，并修复了一些问题

const DEFAULT_ERROR_MESSAGE = "Bad Request";
const DEFAULT_ERROR_STATUS = 400;

export function createErrorResponse(
  options: {
    message?: string;
    status?: number;
  } = {}
): Response {
  const statusText = options?.message || DEFAULT_ERROR_MESSAGE;
  const status = options?.status || DEFAULT_ERROR_STATUS;
  return Response.json(statusText, { status, statusText });
}

type Options<Parser = SearchParamsParser> = {
  /** Custom error message for when the validation fails. */
  message?: string;
  /** Status code for thrown request when validation fails. */
  status?: number;
  /** Custom URLSearchParams parsing function. */
  parser?: Parser;
};

/**
 * Type assertion function avoids problems with some bundlers when
 * using `instanceof` to check the type of a `schema` param.
 */
const isZodType = (input: ZodRawShape | ZodTypeAny): input is ZodTypeAny => {
  return typeof input.parse === "function";
};

/**
 * Generic return type for parseX functions.
 */
type ParsedData<T extends ZodRawShape | ZodTypeAny> = T extends ZodTypeAny
  ? output<T>
  : T extends ZodRawShape
  ? output<ZodObject<T>>
  : never;

/**
 * Generic return type for parseXSafe functions.
 */
type SafeParsedData<T extends ZodRawShape | ZodTypeAny> = T extends ZodTypeAny
  ? SafeParseReturnType<z.infer<T>, ParsedData<T>>
  : T extends ZodRawShape
  ? SafeParseReturnType<ZodObject<T>, ParsedData<T>>
  : never;

/**
 * Parse and validate URLSearchParams or a Request. Throws an error if validation fails.
 * @param request - A Request or URLSearchParams
 * @param schema - A Zod object shape or object schema to validate.
 * @throws {Response} - Throws an error Response if validation fails.
 */
export function parseQuery<T extends ZodRawShape | ZodTypeAny>(
  request: Request | URLSearchParams,
  schema: T,
  options?: Options
): ParsedData<T> {
  try {
    const searchParams = isURLSearchParams(request) ? request : getSearchParamsFromRequest(request);
    const params = parseSearchParams(searchParams, options?.parser);
    const finalSchema = isZodType(schema) ? schema : z.object(schema);
    return finalSchema.parse(params);
  } catch (error) {
    throw createErrorResponse(options);
  }
}
export const zq = {
  mmid: () =>
    z.string().transform((val, ctx) => {
      if (val.endsWith(".dweb") === false) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `[${ctx.path.join(".")}] invalid mmid `,
        });
        return z.NEVER;
      }
      return val as $MMID;
    }),
  string: () => z.string(),
  number: (float = true) =>
    z.string().transform((val, ctx) => {
      const num = float ? parseFloat(val) : parseInt(val);
      if (isFinite(num)) {
        return num;
      }
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `[${ctx.path.join(".")}] fail to parse to number`,
      });

      return z.NEVER;
    }),
  boolean: () => z.string().transform((val) => /^true$/i.test(val)),
  transform: <NewOut>(transform: (arg: string, ctx: RefinementCtx) => NewOut | Promise<NewOut>) =>
    z.string().transform(transform),
  parseQuery,
};

/**
 * Parse and validate URLSearchParams or a Request. Doesn't throw if validation fails.
 * @param request - A Request or URLSearchParams
 * @param schema - A Zod object shape or object schema to validate.
 * @returns {SafeParseReturnType} - An object with the parsed data or a ZodError.
 */
export function parseQuerySafe<T extends ZodRawShape | ZodTypeAny>(
  request: Request | URLSearchParams,
  schema: T,
  options?: Options
): SafeParsedData<T> {
  const searchParams = isURLSearchParams(request) ? request : getSearchParamsFromRequest(request);
  const params = parseSearchParams(searchParams, options?.parser);
  const finalSchema = isZodType(schema) ? schema : z.object(schema);
  return finalSchema.safeParse(params) as SafeParsedData<T>;
}

/**
 * Parse and validate FormData from a Request. Throws an error if validation fails.
 * @param request - A Request or FormData
 * @param schema - A Zod object shape or object schema to validate.
 * @throws {Response} - Throws an error Response if validation fails.
 */
export async function parseForm<T extends ZodRawShape | ZodTypeAny, Parser extends SearchParamsParser<any>>(
  request: Request | FormData,
  schema: T,
  options?: Options<Parser>
): Promise<ParsedData<T>> {
  try {
    const formData = isFormData(request) ? request : await request.clone().formData();
    const data = await parseFormData(formData, options?.parser);
    const finalSchema = isZodType(schema) ? schema : z.object(schema);
    return await finalSchema.parseAsync(data);
  } catch (error) {
    throw createErrorResponse(options);
  }
}

/**
 * Parse and validate FormData from a Request. Doesn't throw if validation fails.
 * @param request - A Request or FormData
 * @param schema - A Zod object shape or object schema to validate.
 * @returns {SafeParseReturnType} - An object with the parsed data or a ZodError.
 */
export async function parseFormSafe<T extends ZodRawShape | ZodTypeAny, Parser extends SearchParamsParser<any>>(
  request: Request | FormData,
  schema: T,
  options?: Options<Parser>
): Promise<SafeParsedData<T>> {
  const formData = isFormData(request) ? request : await request.clone().formData();
  const data = await parseFormData(formData, options?.parser);
  const finalSchema = isZodType(schema) ? schema : z.object(schema);
  return finalSchema.safeParseAsync(data) as Promise<SafeParsedData<T>>;
}

/**
 * The data returned from parsing a URLSearchParams object.
 */
type ParsedSearchParams = Record<string, string | string[]>;

/**
 * Function signature to allow for custom URLSearchParams parsing.
 */
type SearchParamsParser<T = ParsedSearchParams> = (searchParams: URLSearchParams) => T;

/**
 * Check if an object entry value is an instance of Object
 */
function isObjectEntry([, value]: [string, FormDataEntryValue]) {
  return value instanceof Object;
}

/**
 * Get the form data from a request as an object.
 */
function parseFormData(formData: FormData, customParser?: SearchParamsParser) {
  const objectEntries = [...formData.entries()].filter(isObjectEntry);
  objectEntries.forEach(([key, value]) => {
    formData.set(key, JSON.stringify(value));
  });
  // Context on `as any` usage: https://github.com/microsoft/TypeScript/issues/30584
  return parseSearchParams(new URLSearchParams(formData as any), customParser);
}

/**
 * Get the URLSearchParams as an object.
 */
function parseSearchParams(searchParams: URLSearchParams, customParser?: SearchParamsParser): ParsedSearchParams {
  const parser = customParser || parseSearchParamsDefault;
  return parser(searchParams);
}

/**
 * The default parser for URLSearchParams.
 * Get the search params as an object. Create arrays for duplicate keys.
 */
const parseSearchParamsDefault: SearchParamsParser = (searchParams) => {
  const values: ParsedSearchParams = {};
  searchParams.forEach((value, key) => {
    const currentVal = values[key];
    if (currentVal && Array.isArray(currentVal)) {
      currentVal.push(value);
    } else if (currentVal) {
      values[key] = [currentVal, value];
    } else {
      values[key] = value;
    }
  });
  return values;
};

/**
 * Get the search params from a request.
 */
function getSearchParamsFromRequest(request: Request): URLSearchParams {
  const url = new URL(request.url);
  return url.searchParams;
}

/**
 * Check if value is an instance of FormData.
 * This is a workaround for `instanceof` to support multiple platforms.
 */
function isFormData(value: unknown): value is FormData {
  return getObjectTypeName(value) === "FormData";
}

/**
 * Check if value is an instance of URLSearchParams.
 * This is a workaround for `instanceof` to support multiple platforms.
 */
function isURLSearchParams(value: unknown): value is URLSearchParams {
  return getObjectTypeName(value) === "URLSearchParams";
}

function getObjectTypeName(value: unknown): string {
  return toString.call(value).slice(8, -1);
}
