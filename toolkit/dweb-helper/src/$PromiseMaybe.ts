
export type $PromiseMaybe<T> = Promise<Awaited<T>> | Awaited<T>;
