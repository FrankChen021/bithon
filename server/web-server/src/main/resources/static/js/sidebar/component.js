
class MetricSidebar {
    constructor(containerId, appName) {
        this._container = $('#' + containerId);
        this._appName = appName;
    }

    load() {
        $.ajax({
            type: 'POST',
            url: apiHost + "/api/datasource/name",
            data: JSON.stringify({}),
            async: true,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                $.each(data, (index, item)=>{
                    this.addMetricItem(item);
                });
            },
            error: (data) => {
            }
        });
    }

    select() {
    }

    // PRIVATE
    addMetricItem(item) {
        this._container.append(`<a href="/ui/app/${appName}/${item.name}">${item.displayText}</a>`);
    }
}