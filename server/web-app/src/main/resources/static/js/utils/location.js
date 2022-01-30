
function extractQueryParameters() {
    const parameters = {};
    const uri = decodeURI(window.location.href);
    const paramPos = uri.indexOf('?') + 1;

    if (paramPos <= 0) {
        return parameters;
    }

    // extract args
    const args = uri.substring(paramPos).split("&")
    for(let i = 0; i < args.length; i++) {
        const pair = args[i].split("=");
        parameters[pair[0]] = pair[1];
    }

    return parameters;
}

window.queryParams = extractQueryParameters();