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
            redirect(`/web/metrics/${item.id}`);
        });
        this._container.append(i);
    }
}
