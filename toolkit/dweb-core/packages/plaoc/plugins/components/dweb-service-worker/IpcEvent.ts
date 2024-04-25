
export class PlaocEvent extends Event {
  constructor(readonly eventName:string,readonly data: string) {
    super(eventName);
  }
}