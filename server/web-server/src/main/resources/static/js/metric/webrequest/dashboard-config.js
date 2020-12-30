var webRequestMetricsDashboard = {
    "title": "",
    "dataSource": "web-request-metrics",
    "charts":[{
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
