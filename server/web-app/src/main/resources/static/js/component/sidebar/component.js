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
            window.location = `${item.id}?interval=${g_MetricSelectedInterval}`;
        });
        this._container.append(i);
    }
}
