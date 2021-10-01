
class DashboardApi {

    getDashboardConfig(boardName, successCallback, errorCallback) {
        $.ajax({
            type: 'GET',
            url: "/web/api/dashboard/get/" + boardName,
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