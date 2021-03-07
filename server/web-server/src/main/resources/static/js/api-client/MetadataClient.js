
class MetadataClient {

    constructor(apiHost) {
        this._apiHost = apiHost;
    }

    getApplications(successCallback, errorCallback) {
        $.ajax({
            url: this._apiHost + "/api/meta/getMetadataList",
            data: JSON.stringify({ type: 'APPLICATION' }),
            type: "POST",
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
}
