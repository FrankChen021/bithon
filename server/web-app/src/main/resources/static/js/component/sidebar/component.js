
class MetricSidebar {
    constructor(containerId, appName) {
        this._container = $('#' + containerId);
        this._appName = appName;
    }

    load() {
        const data = [{
            id:  'jvm-metrics',
            text: 'JVM'
        },{
            id:  'thread-pool-metrics',
            text: 'Thread Pool'
        },{
            id: 'web-server-metrics',
            text: 'Web Server'
        },{
            id: 'web-request-metrics',
            text: 'Web Request'
        },{
            id: 'http-outgoing-metrics',
            text: 'Outgoing HTTP'
        },{
            id: 'jdbc-pool-metrics',
            text: 'JDBC Connection Pool'
        },{
            id: 'sql-metrics',
            text: 'SQL'
        },{
            id: 'mongodb-metrics',
            text: 'MongoDb'
        },{
            id: 'redis-metrics',
            text: 'Redis'
        }];
        $.each(data, (index, item)=>{
            this.addMetricItem(item);
        });
    }

    select() {
    }

    // PRIVATE
    addMetricItem(item) {
        this._container.append(`<a href="/ui/app/metric/${appName}/${item.id}">${item.text}</a>`);
    }
}