function toQueryParameters(query) {
    const parameters = {};
    const uri = decodeURI(query);
    const paramPos = uri.indexOf('?') + 1;

    if (paramPos <= 0) {
        return parameters;
    }

    // extract args
    const args = uri.substring(paramPos).split("&")
    for (let i = 0; i < args.length; i++) {
        const pair = args[i].split("=");
        const name = pair[0].trim();
        const value = pair[1];
        if (name === '') {
            continue;
        }

        parameters[name] = value;
    }

    return parameters;
}

window.queryParams = toQueryParameters(window.location.href);