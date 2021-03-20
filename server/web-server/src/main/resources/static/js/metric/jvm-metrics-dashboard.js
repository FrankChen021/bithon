var jvm_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "jvm-metrics",
        "title": "CPU",
        "width": 2, //1,2,3,4
        "yAxis": [ {
                "unit": "percentage"
             }
         ],
        "metrics":[{
            "name": "processCpuLoad",
            "unit": "percentage", //percentage,binary_byte,decimal_byte,
            "chartType": "line"   //line/bar/area
        }]
    },{
        "dataSource": "jvm-metrics",
        "title": "Threads",
        "width": 2,
        "metrics":[{
          "name": "activeThreads",
        },{
          "name": "totalThreads"
        }]
    },{
        "dataSource": "jvm-metrics",
        "title": "Memory",
        "width": 4,
        "yAxis": [ {
            "minInterval": 1024 * 1024,
            "interval": 1024 * 1024 * 1024,
            "unit": "binary_byte"
        }],
        "metrics":[{
            "name": "heap",
        },{
            "name": "heapUsed"
        },{
            "name": "heapCommitted"
        }]
     },{
        "dataSource": "jvm-gc-metrics",
        "title": "Garbage Collection",
        "width": 4,
        "yAxis": [{
        },{
              "unit": "microsecond"
        }],
        "metrics":[{
            "name": "gcCount",
        },{
            "name": "avgGcTime",
            "yAxis": 1
       }]
    }]
}
