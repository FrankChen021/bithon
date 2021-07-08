var web_request_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "web-request-metrics",
        "title": "QPS",
        "width": 4, //1,2,3,4
        "yAxis": [ {
          },{}
        ],
        "metrics":[{
            "name": "qps"
        },{
            "name": "callCount",
            "yAxis": 1
        }]
    },{
        "dataSource": "web-request-metrics",
        "title": "Response Time",
        "width": 4, //1,2,3,4
        "yAxis": [ {
            "unit": "millisecond"
          }
        ],
        "metrics":[{
          "name": "avgResponseTime"
        },{
            "name": "minResponseTime"
        },{
            "name": "maxResponseTime"
        }]
    }, {
        "dataSource": "web-request-metrics",
        "title": "Errors",
        "width": 4, //1,2,3,4
        "metrics":[{
            "name": "count4xx"
        },{
            "name": "count5xx"
        },{
            "name": "flowedCount"
        },{
            "name": "degradedCount"
        }]
    }]
}
