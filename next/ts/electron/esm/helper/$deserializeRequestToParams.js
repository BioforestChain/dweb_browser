import { $typeNameParser } from "./$typeNameParser.js";
export const $deserializeRequestToParams = (schema) => {
    return (request) => {
        const url = request.parsed_url;
        const params = {};
        for (const [key, typeName2] of Object.entries(schema)) {
            params[key] = $typeNameParser(key, typeName2, url.searchParams.get(key));
        }
        return params;
    };
};
