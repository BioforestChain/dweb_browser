/// <reference lib="dom"/>
type $Response =Awaited< ReturnType<typeof fetch>>
export interface SafeResponse {
  new (body?: BodyInit | null | undefined, init?: ResponseInit | undefined): $Response;
  prototype: $Response;
  error(): $Response;
  // deno-lint-ignore no-explicit-any
  json(data: any, init?: ResponseInit | undefined): $Response;
  redirect(url: string | URL, status?: number | undefined): $Response;
}
const _Response = self.Response as  SafeResponse
export { _Response as Response };

