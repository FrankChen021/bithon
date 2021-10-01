
class DashboardApi {

    getDashboardConfig(boardName, successCallback, errorCallback) {
        $.ajax({
            type: 'GET',
            url: "/ui/api/dashboard/" + boardName,
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