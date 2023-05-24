import { fetchBaseExtends } from "./$makeFetchBaseExtends.js";
import { fetchStreamExtends } from "./$makeFetchStreamExtends.js";

export const fetchExtends = {
  ...fetchBaseExtends,
  ...fetchStreamExtends,
};
