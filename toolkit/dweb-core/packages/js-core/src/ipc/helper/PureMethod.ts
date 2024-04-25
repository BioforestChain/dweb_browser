export const enum PURE_METHOD {
  GET = "GET",
  POST = "POST",
  PUT = "PUT",
  DELETE = "DELETE",
  OPTIONS = "OPTIONS",
  TRACE = "TRACE",
  PATCH = "PATCH",
  PURGE = "PURGE",
  HEAD = "HEAD",
}
export const toPureMethod = (method?: string) => {
  if (method == null) {
    return PURE_METHOD.GET;
  }

  switch (method.toUpperCase()) {
    case PURE_METHOD.GET: {
      return PURE_METHOD.GET;
    }
    case PURE_METHOD.POST: {
      return PURE_METHOD.POST;
    }
    case PURE_METHOD.PUT: {
      return PURE_METHOD.PUT;
    }
    case PURE_METHOD.DELETE: {
      return PURE_METHOD.DELETE;
    }
    case PURE_METHOD.OPTIONS: {
      return PURE_METHOD.OPTIONS;
    }
    case PURE_METHOD.TRACE: {
      return PURE_METHOD.TRACE;
    }
    case PURE_METHOD.PATCH: {
      return PURE_METHOD.PATCH;
    }
    case PURE_METHOD.PURGE: {
      return PURE_METHOD.PURGE;
    }
    case PURE_METHOD.HEAD: {
      return PURE_METHOD.HEAD;
    }
  }
  throw new Error(`invalid method: ${method}`);
};
