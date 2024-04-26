import { fetchBaseExtends } from "./$makeFetchBaseExtends.ts";
import { fetchStreamExtends } from "./$makeFetchStreamExtends.ts";

export const fetchExtends = {
  ...fetchBaseExtends,
  ...fetchStreamExtends,
};
