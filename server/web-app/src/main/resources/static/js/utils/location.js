function toQueryParameters(uri) {
    const parameters = {};
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

        parameters[name] = decodeURIComponent(value);
    }

    if (parameters['appName'] != null) {
        g_SelectedApp = parameters['appName'];
    }
    if(parameters['instanceName'] != null) {
        g_SelectedInstance = parameters['instanceName'];
    }

    return parameters;
}

window.queryParams = toQueryParameters(window.location.href);