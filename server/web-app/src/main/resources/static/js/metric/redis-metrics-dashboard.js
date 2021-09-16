const redis_metrics_dashboard = {
    "title": "",
    "charts": [{
        "dataSource": "redis-metrics",
        "title": "Commands",
        "width": 4, //1,2,3,4
        "metrics": [{
            "name": "totalCount"
        }]
    }, {
        "dataSource": "redis-metrics",
        "title": "Time",
        "width": 4, //1,2,3,4
        "yAxis": [{
            "unit": "nano2Millisecond"
        }],
        "metrics": [{
            "name": "minRequestTime"
        }, {
            "name": "avgRequestTime"
        }, {
            "name": "maxRequestTime"
        }, {
            "name": "minResponseTime"
        }, {
            "name": "avgResponseTime"
        }, {
            "name": "maxResponseTime"
        }]
    }, {
        "dataSource": "redis-metrics",
        "title": "IO",
        "width": 4, //1,2,3,4
        "yAxis": [{
            "minInterval": 1024 * 1024,
            "interval": 1024 * 1024 * 1024,
            "unit": "binary_byte"
        }],
        "metrics": [{
            "name": "requestBytes"
        }, {
            "name": "responseBytes"
        }]
    }]
}
