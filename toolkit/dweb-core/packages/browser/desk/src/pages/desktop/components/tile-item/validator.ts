export const validatorTileSize = (value: unknown) => {
  if (typeof value === "string") {
    return parseFloat(value) + "%" === value;
  } else if (typeof value === "number") {
    return Number.isSafeInteger(value) && value > 0;
  }
  return false;
};
export const validatorPosition = (value: unknown) => {
  if (typeof value === "number") {
    return Number.isSafeInteger(value) && value > 0;
  }
  return false;
};
