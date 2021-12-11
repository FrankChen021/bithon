class MetricSidebar {
    constructor(containerId, appName) {
        this._container = $('#' + containerId);
        this._appName = appName;
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
        const i = $(`<a href="#">${item.text}</a>`).click(() => {
            let url = `/web/app/metric/${this._appName}/${item.id}?interval=${g_MetricSelectedInterval}`;
            if ( g_SelectedInstance != null ) {
                url += `&instance=${g_SelectedInstance}`;
            }
            window.location = url;
        });
        this._container.append(i);
    }
}
