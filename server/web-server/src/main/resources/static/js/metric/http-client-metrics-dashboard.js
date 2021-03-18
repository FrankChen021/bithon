var http_client_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "http-client-metrics",
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
          "name": "avgResponseTime",
          "yAxis": 1
        }]
    },{
        "dataSource": "http-client-metrics",
        "title": "Error",
        "width": 4, //1,2,3,4
        "metrics":[{
          "name": "count4xx"
        },{
           "name": "count5xx"
        },{
           "name": "countException"
        }]
     },{
        "dataSource": "http-client-metrics",
        "title": "IO",
        "width": 4, //1,2,3,4
        "yAxis": [ {
           "minInterval": 1024 * 1024,
           "interval": 1024 * 1024 * 1024,
           "formatter": "binary_byte"
        }],
        "metrics":[{
            "name": "requestBytes"
        },{
          "name": "responseBytes"
        }]
    }]
}
