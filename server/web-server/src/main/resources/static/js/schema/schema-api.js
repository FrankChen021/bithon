
class SchemaApi {

    getNames(successCallback, errorCallback) {
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/datasource/name",
            data: JSON.stringify({}),
            async: true,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                successCallback(data);
            },
            error: (data) => {
                errorCallback(data);
            }
        });
    }

    getSchema(name, successCallback, errorCallback) {
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/datasource/schema/" + name,
            data: JSON.stringify({}),
            async: true,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                successCallback(data);
            },
            error: (data) => {
                errorCallback(data);
            }
        });
    }

    getSchemas(name, successCallback, errorCallback) {
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/datasource/schemas",
            data: JSON.stringify({}),
            async: true,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                callback(data);
            },
            error: (data) => {
                errorCallback(data);
            }
        });
    }
}