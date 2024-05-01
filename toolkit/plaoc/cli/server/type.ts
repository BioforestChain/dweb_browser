import { ArgumentValue, Type, ValidationError } from "../deps/cliffy.ts";

export class HostType extends Type<string> {
  public parse({ label, name, value }: ArgumentValue): string {
    if (!this.isIP(value) && !this.isHost(value)) {
      throw new ValidationError(`${label} "${name}" ${value} is not a valid ip address.`);
    }

    return value;
  }
  private isIP(str: string) {
    return /^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$/.test(
      str
    );
  }
  private isHost(str: string) {
    return /^(?!\.)([a-zA-Z0-9\-]{0,62}[a-zA-Z0-9]\.){1,126}(?!\.)([a-zA-Z0-9]{1,63})$/.test(str);
  }
}
