import { simpleDecoder, simpleEncoder } from "../encoding.ts";
const random_base = [
  BigInt("0x982f8a4291443771cffbc0b5a5dbb5e95bc25639f111f159a4823f92d55e1cab"),
  BigInt("0x98aa07d8015b8312be853124c37d0c55745dbe72feb1de80a706dc9b74f19bc1"),
  BigInt("0xc1699be48647beefc69dc10fcca10c246f2ce92daa84744adca9b05cda88f976"),
  BigInt("0x52513e986dc631a8c82703b0c77f59bff30be0c64791a7d55163ca0667292914"),
  BigInt("0x850ab72738211b2efc6d2c4d130d385354730a65bb0a6a762ec9c281852c7292"),
  BigInt("0xa1e8bfa24b661aa8708b4bc2a3516cc719e892d1240699d685350ef470a06a10"),
  BigInt("0x16c1a419086c371e4c774827b5bcb034b30c1c394aaad84e4fca9c5bf36f2e68"),
  BigInt("0xee828f746f63a5781478c8840802c78cfaffbe90eb6c50a4f7a3f9bef27871c6"),
];

const max256 = BigInt("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

export interface RandomNumberOptions {
  seed?: number | string;
  min?: number;
  max?: number;
  step?: number;
}
export const randomStringToNumber = (value: string, options: RandomNumberOptions = {}): number => {
  const { min = 0, max = 100, step = 1 } = options;
  let { seed = 1 } = options;
  if (typeof seed === "string") {
    seed = randomStringToNumber(seed, { min: 0, max: Number.MAX_SAFE_INTEGER });
  }
  if (Number.isSafeInteger(seed) === false || seed === 0) {
    seed = 1;
  }
  const seed_bigint = BigInt(seed);
  let value_bigint = BigInt("0x" + simpleDecoder(simpleEncoder(value, "utf8"), "hex").padStart(64, "f"));
  while (value_bigint > max256) {
    value_bigint = (value_bigint % max256) + (value_bigint >> 256n);
  }
  let hash_value = value_bigint;
  for (const [index, item] of random_base.entries()) {
    hash_value = (hash_value / item + seed_bigint) | (value_bigint >> BigInt(index * 32));
  }

  const range_size = BigInt((max - min) / step);
  const result_base = Number((hash_value + seed_bigint) % range_size);
  return result_base * step + min;
};
