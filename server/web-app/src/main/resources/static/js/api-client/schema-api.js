
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

    getDimensions(dataSource,
                  startTime,
                  endTime,
                  conditions,
                  dimension,
                  successCallback, errorCallback) {
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/datasource/dimensions",
            data: JSON.stringify({
                dataSource: dataSource,
                conditions: conditions,
                startTimeISO8601: moment().utc().subtract(10, 'minute').local().toISOString(),
                endTimeISO8601: moment().utc().local().toISOString(),
            }),
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