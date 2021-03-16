var web_request_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "web-request-metrics",
        "title": "Requests",
        "width": 4, //1,2,3,4
        "yAxis": [ {
            },{
            "formatter": "millisecond"
          }
        ],
        "metrics":[{
            "name": "requestCount"
        },{
          "name": "avgCostTime",
          "yAxis": 1
        }]
    }]
}
