export const $typeNameParser = (key, typeName2, value) => {
    let param;
    if (value === null) {
        if (typeName2.endsWith("?")) {
            param = undefined;
        }
        else {
            throw new Error(`param type error: '${key}'.`);
        }
    }
    else {
        const typeName1 = (typeName2.endsWith("?") ? typeName2.slice(0, -1) : typeName2);
        switch (typeName1) {
            case "number": {
                param = +value;
                break;
            }
            case "boolean": {
                param = value === "" || value === "false" ? false : Boolean(value.toLowerCase());
                break;
            }
            case "mmid": {
                if (value.endsWith(".dweb") === false) {
                    throw new Error(`param mmid type error: '${key}':'${value}'`);
                }
                param = value;
                break;
            }
            case "string": {
                param = value;
                break;
            }
            case "object": {
                param = JSON.parse(value);
                break;
            }
            default:
                param = void 0;
        }
    }
    return param;
};
