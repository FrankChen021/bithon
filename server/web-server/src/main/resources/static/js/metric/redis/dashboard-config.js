var redisMetricsDashboard = {
    "title": "",
    "dataSource": "redis-metrics",
    "charts":[{
        "title": "Commands",
        "width": 4, //1,2,3,4
        "metrics":[{
            "name": "totalCount"
        }]
    },{
         "title": "Time",
         "width": 4, //1,2,3,4
         "yAxis": [{
             "formatter": "nano2Millisecond"
         }],
         "metrics":[{
            "name": "requestTime"
         },{
            "name": "responseTime"
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
