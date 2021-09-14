const http_incoming_metrics_dashboard = {
    "title": "",
    "charts": [{
        "dataSource": "http-incoming-metrics",
        "title": "QPS",
        "width": 4, //1,2,3,4
        "yAxis": [{}, {}
        ],
        "metrics": [{
            "name": "qps"
        }, {
            "name": "okQPS"
        }, {
            "name": "totalCount",
            "yAxis": 1
        }]
    }, {
        "dataSource": "http-incoming-metrics",
        "title": "Response Time",
        "width": 4, //1,2,3,4
        "yAxis": [{
            "unit": "millisecond"
        }],
        "metrics": [{
            "name": "avgResponseTime"
        }, {
            "name": "minResponseTime"
        }, {
            "name": "maxResponseTime"
        }]
    }, {
        "dataSource": "http-incoming-metrics",
        "title": "Errors",
        "width": 4, //1,2,3,4
        "metrics": [{
            "name": "count4xx"
        }, {
            "name": "count5xx"
        }, {
            "name": "flowedCount"
        }, {
            "name": "degradedCount"
        }]
    }, {
        "dataSource": "http-incoming-metrics",
        "title": "IO",
        "width": 4, //1,2,3,4
        "yAxis": [{
            //"minInterval": 1000 * 1000,
            //"interval": 1000 * 1000 * 1000,
            "unit": "compact_number"
        }],
        "metrics": [{
            "name": "requestBytes"
        }, {
            "name": "responseBytes"
        }]
    }]
};
