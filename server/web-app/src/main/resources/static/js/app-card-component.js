class AppCardComponent {

    constructor(host, id) {
        this._id = id + '_cardComponent';
        this._container = $('#' + id).append('<div class="row row-cols-md-4">').find('div').attr('id', this._id);
        this._apiHost = host;
    }

    loadAppList() {
        this.#loadAppOverview();
        setInterval(() => {
            this.#loadAppOverview();
        }, 10000);
    }

    // private
    #getOrCreateAppCard(appName) {
        let appCard = $('#' + appName);
        if (appCard.length === 0) {
            const cardContainer = $('<div class="col mb-4">                             ' +
                '    <div class="card card-block">                             ' +
                '        <div class="card-body">                    ' +
                '            <h5 class="card-title app-name">Card title</h5> ' +
                '            <small class="text-muted instance-count"></small><br/>' +
                '            <small class="text-muted start-time"></small><br/>' +
                '            <small class="text-muted up-time"></small> ' +
                '        </div>                                     ' +
                '    </div>                                         ' +
                '</div>                                             ');

            appCard = cardContainer.find('.card');
            appCard.attr('id', appName);
            cardContainer.find('.app-name').html(`<a href="/web/metrics/jvm-metrics?appName=${appName}">${appName}</a>`);
            this._container.append(cardContainer);
        }
        return appCard;
    }

    // private
    #loadAppOverview() {
        $.ajax({
            url: this._apiHost + "/api/datasource/groupBy/v3",
            data: JSON.stringify({
                dataSource: 'jvm-metrics',
                fields: [
                    "appName",
                    "instanceStartTime",
                    {
                        name: "instanceCount",
                        field: "instanceName",
                        aggregator: "cardinality"
                    }
                ],
                groupBy: ["appName"],
                interval: {
                    startISO8601: moment().utc().subtract(12, 'hour').local().toISOString(),
                    endISO8601: moment().utc().local().toISOString()
                },
                orderBy: {
                    name: "appName",
                    order: "asc"
                }
            }),
            type: "POST",
            async: true,
            contentType: "application/json",
            success: (data) => {
                const items = data.data;
                $.each(items, (index, overview) => {
                    const appCard = this.#getOrCreateAppCard(overview.appName);
                    $(appCard).find('.instance-count').html('<b>Instances</b>：' + overview.instanceCount);
                    $(appCard).find('.start-time').html('<b>Started at</b>：' + moment(overview.instanceStartTime).local().format('YYYY-MM-DD HH:mm:ss'));
                    $(appCard).find('.up-time').html('<b>Up Time</b>：' + (new Date().getTime() - overview.instanceStartTime).formatTimeDuration());
                });
            }
        });
    }


}