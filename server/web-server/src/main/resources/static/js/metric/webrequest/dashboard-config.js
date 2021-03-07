var webRequestMetricsDashboard = {
    "title": "",
    "charts":[{
        "dataSource": "web-request-metrics",
        "title": "Requests",
        "width": 4, //1,2,3,4
        "yAxis": [ {
            },{
            "formatter": "nano2Millisecond"
          }
        ],
        "metrics":[{
            "name": "requestCount"
        },{
          "name": "costTime",
          "yAxis": 1
        }]
    }]
}
