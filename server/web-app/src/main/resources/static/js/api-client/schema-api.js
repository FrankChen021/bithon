
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

    /**
     * name, async, successCallback, errorCallback
     * @param option
     */
    getSchema(option) {
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/datasource/schema/" + option.name,
            data: JSON.stringify({}),
            async: option.async === undefined ? true : option.async,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                option.successCallback(data);
            },
            error: (data) => {
                option.errorCallback(data);
            }
        });
    }
}
