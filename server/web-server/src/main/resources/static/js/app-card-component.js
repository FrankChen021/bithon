
class AppCardComponent {

    constructor(host, id) {
        this._id = id + '_cardComponent';
        this._container = $('#'+id).append('<div class="row row-cols-md-4" style="padding-top:80px; margin-top: auto;margin-bottom: auto;"">').find('div').attr('id', this._id);
        this._apiHost = host;
    }

    loadAppList() {
        $.ajax({
            url: this._apiHost + "/api/meta/getMetadataList",
            data: JSON.stringify({ type: 'APPLICATION' }),
            type: "POST",
            async: true,
            contentType: "application/json",
            success: (data) => {
                this._appList = data;
                console.log(this._appList);

                var card = '<div class="col mb-4">                             ' +
                           '    <div class="card card-block">                             ' +
                           '        <div class="card-body">                    ' +
                           '            <h5 class="card-title app-name">Card title</h5> ' +
                           '            <small class="text-muted instance-count"></small><br/>' +
                           '            <small class="text-muted start-time"></small><br/>' +
                           '            <small class="text-muted up-time"></small> ' +
                           '        </div>                                     ' +
                           '    </div>                                         ' +
                           '</div>                                             ';
                $.each(data, (index, app)=>{
                    this._container.append(card)
                        .find('.card').attr('id', app.name)
                        .find('.app-name').html('<a href="#">'+app.name+'</a>').click(()=> {
                            window.location.href = '/ui/app/metric/' + app.name + '/jvm-metrics';
                        });
                });

                this.loadAppOverview();
                setInterval(()=>{
                    this.loadAppOverview();
                }, 10000);
            }
        });
    }

    // private
    loadAppOverview() {
        $.ajax({
            url: this._apiHost + "/api/datasource/sql",
            data: JSON.stringify({
                dataSource: 'jvm-metrics',
                sql: 'SELECT "appName", COUNT(DISTINCT "instanceName") "instanceCount", min("instanceStartTime") "instanceStartTime"' +
                     ' FROM "bithon_jvm_metrics" WHERE "timestamp" >= \'' + moment().utc().subtract(5, 'hour').local().toISOString() + '\' AND "timestamp" < \'' + moment().utc().local().toISOString() + '\' GROUP BY "appName"'
            }),
            type: "POST",
            async: true,
            contentType: "application/json",
            success: (data) => {
                console.log(data);

                $.each(data, (index, overview)=>{
                    var appCard = $('#' + overview.appName);
                    $(appCard).find('.instance-count').text('实例个数：' + overview.instanceCount);
                    $(appCard).find('.start-time').text('启动时间：' + moment(overview.instanceStartTime).local().format('YYYY-MM-DD HH:mm:ss'));
                    $(appCard).find('.up-time').text('运行时长：' + this.timeDiff(overview.instanceStartTime) );
                });
            }
        });
    }

    timeDiff(before) {
        var diff = new Date().getTime() - before;
        var days=Math.floor(diff/(24*3600*1000))

        diff=diff%(24*3600*1000)    //计算天数后剩余的毫秒数
        var hours=Math.floor(diff/(3600*1000))

        diff=diff%(3600*1000)
        var minutes=Math.floor(diff/(60*1000))

        var diff=diff%(60*1000)      //计算分钟数后剩余的毫秒数
        var seconds=Math.round(diff/1000)

        var text = '';
        if ( days > 0 )
            text += days + '天';
        if ( text.length > 0 || hours != 0 )
            text += hours + '小时';
        if ( text.length > 0 || minutes > 0 )
            text += minutes + '分钟';
        if ( text.length > 0 || seconds > 0 )
            text += seconds + '秒';

        return text;
    }
}