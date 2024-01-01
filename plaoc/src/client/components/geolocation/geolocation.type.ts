export interface $GeolocationPosition {
  state: $GeolocationPositionState;
  coords: GeolocationCoordinates;
  timestamp: number;
}
export interface $GeolocationPositionState {
  code: number;
  message: string | null;
}
