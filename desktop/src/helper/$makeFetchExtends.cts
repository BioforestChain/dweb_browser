import { fetchBaseExtends } from "./$makeFetchBaseExtends.cjs";
import { fetchStreamExtends } from "./$makeFetchStreamExtends.cjs";

export const fetchExtends = {
  ...fetchBaseExtends,
  ...fetchStreamExtends,
};
