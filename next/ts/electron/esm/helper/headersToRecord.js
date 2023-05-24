export const headersToRecord = (headers) => {
    let record = Object.create(null);
    if (headers) {
        let req_headers;
        if (headers instanceof Array) {
            req_headers = new Headers(headers);
        }
        else if (headers instanceof Headers) {
            req_headers = headers;
        }
        else {
            record = headers;
        }
        if (req_headers !== undefined) {
            req_headers.forEach((value, key) => {
                record[key] = value;
            });
        }
    }
    return record;
};
