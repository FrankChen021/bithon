class MetricSidebar {
    constructor(containerId, appName) {
        this._container = $('#' + containerId);
    }

    load() {
        g_DashboardApi.getDashboardList(
            (data) => {
                $.each(data, (index, item) => {
                    this.addDashboardItem({id: item.value, text: item.text});
                });
            },
            (data) => {
            });
    }

    select() {
    }

    // PRIVATE
    addDashboardItem(item) {
        const i = $(`<a>${item.text}</a>`).click(() => {
            let url = `/web/metrics/${item.id}?appName=${g_SelectedApp}&interval=${g_MetricSelectedInterval}`;
            if ( g_SelectedInstance != null ) {
                url += `&instanceName=${g_SelectedInstance}`;
            }
            window.location = url;
        });
        this._container.append(i);
    }
}
