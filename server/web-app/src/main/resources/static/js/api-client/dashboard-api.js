
class DashboardApi {

    getDashboardConfig(boardName, successCallback, errorCallback) {
        $.ajax({
            type: 'GET',
            url: "/api/dashboard/get/" + boardName,
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

    getDashboardList(folder, successCallback, errorCallback) {
        $.ajax({
            type: 'GET',
            url: `/api/dashboard/names?folder=${folder}`,
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

const g_DashboardApi = new DashboardApi();
