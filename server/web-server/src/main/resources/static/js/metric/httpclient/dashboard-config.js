var httpClientMetricsDashboard = {
    "title": "",
    "dataSource": "http-client-metrics",
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
    },{
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
