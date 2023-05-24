export const httpMethodCanOwnBody = (method) => {
    return (method !== "GET" &&
        method !== "HEAD" &&
        method !== "TRACE" &&
        method !== "OPTIONS");
};
