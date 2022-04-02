class AppCardComponent {

    constructor(host, id) {
        this._id = id + '_cardComponent';
        this._container = $('#' + id).append('<div class="row row-cols-md-4" style="padding-top:80px; margin-top: auto;margin-bottom: auto;"">').find('div').attr('id', this._id);
        this._apiHost = host;
    }

    loadAppList() {
        $.ajax({
            url: this._apiHost + "/api/meta/getMetadataList",
            data: JSON.stringify({type: 'APPLICATION'}),
            type: "POST",
            async: true,
            contentType: "application/json",
            success: (data) => {
                this._appList = data;
                console.log(this._appList);

                $.each(data, (index, app) => {
                    this.getOrCreateAppCard(app.applicationName);
                });

                this.loadAppOverview();
                setInterval(() => {
                    this.loadAppOverview();
                }, 10000);
            }
        });
    }

    // private
    getOrCreateAppCard(appName) {
        const card = $('<div class="col mb-4">                             ' +
            '    <div class="card card-block">                             ' +
            '        <div class="card-body">                    ' +
            '            <h5 class="card-title app-name">Card title</h5> ' +
            '            <small class="text-muted instance-count"></small><br/>' +
            '            <small class="text-muted start-time"></small><br/>' +
            '            <small class="text-muted up-time"></small> ' +
            '        </div>                                     ' +
            '    </div>                                         ' +
            '</div>                                             ');

        card.find('.card').attr('id', appName);
        card.find('.app-name').html('<a href="#">' + appName + '</a>').click(() => {
            window.location.href = '/web/metrics/jvm-metrics?appName=' + appName;
        });
        this._container.append(card);
    }

    // private
    loadAppOverview() {
        $.ajax({
            url: this._apiHost + "/api/datasource/groupBy",
            data: JSON.stringify({
                dataSource: 'jvm-metrics',
                metrics: ["instanceStartTime"],
                aggregators: [
                    {
                        type: "cardinality",
                        name: "instanceCount",
                        dimension: "instanceName"
                    }
                ],
                startTimeISO8601: moment().utc().subtract(5, 'hour').local().toISOString(),
                endTimeISO8601: moment().utc().local().toISOString(),
                groupBy: ["appName"]
            }),
            type: "POST",
            async: true,
            contentType: "application/json",
            success: (data) => {
                console.log(data);

                $.each(data, (index, overview) => {
                    const appCard = $('#' + overview.appName);
                    $(appCard).find('.instance-count').html('<b>Instances</b>：' + overview.instanceCount);
                    $(appCard).find('.start-time').html('<b>Started at</b>：' + moment(overview.instanceStartTime).local().format('YYYY-MM-DD HH:mm:ss'));
                    $(appCard).find('.up-time').html('<b>Up Time</b>：' + this.timeDiff(overview.instanceStartTime));
                });
            }
        });
    }

    timeDiff(before) {
        let diff = new Date().getTime() - before;
        const days = Math.floor(diff / (24 * 3600 * 1000));

        diff = diff % (24 * 3600 * 1000)    //计算天数后剩余的毫秒数
        const hours = Math.floor(diff / (3600 * 1000));

        diff = diff % (3600 * 1000)
        const minutes = Math.floor(diff / (60 * 1000));

        diff = diff % (60 * 1000);      //计算分钟数后剩余的毫秒数
        const seconds = Math.round(diff / 1000);

        let text = '';
        if (days > 0)
            text += days + ' Day';
        if (text.length > 0 || hours !== 0)
            text += ' ' + hours + ' Hour';
        if (text.length > 0 || minutes > 0)
            text += ' ' + minutes + ' Min';
        if (text.length > 0 || seconds > 0)
            text += ' ' + seconds + ' Sec';

        return text;
    }
}