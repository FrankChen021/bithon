var mongodb_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "mongodb-metrics",
        "title": "Commands",
        "width": 4, //1,2,3,4
        "yAxis": [{},{}],
        "metrics":[{
            "name": "callCount"
        },{
            "name": "tps",
            "yAxis": 1
        }]
    },{
        "dataSource": "mongodb-metrics",
        "title": "Time",
        "width": 4, //1,2,3,4
        "yAxis": [{
            "unit": "nano2Millisecond"
        }],
        "metrics":[{
            "name": "responseTime"
        },{
            "name": "minResponseTime"
        },{
            "name": "maxResponseTime"
        }]
    },{
        "dataSource": "mongodb-metrics",
        "title": "IO",
        "width": 4, //1,2,3,4
        "yAxis": [ {
            "minInterval": 1024 * 1024,
            "interval": 1024 * 1024 * 1024,
            "unit": "binary_byte"
        }],
        "metrics":[{
            "name": "requestBytes"
        },{
            "name": "responseBytes"
        }]
    }]
}
