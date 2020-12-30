var jvmDashboard = {
    "title": "",
    "dataSource": "jvm-metrics",
    "charts":[{
            "title": "CPU",
            "width": 2, //1,2,3,4
            "yAxis": [ {
                    "formatter": "percentage"
                 }
             ],
            "metrics":[{
                "name": "processCpuLoad",
                "unit": "percentage", //percentage,binary_byte,decimal_byte,
                "chartType": "line"   //line/bar/area
            }]
        },{
          "title": "Threads",
          "width": 2,
          "metrics":[{
              "name": "activeThreads",
          },{
              "name": "totalThreads"
          }]
      },{
         "title": "Memory",
         "width": 4,
         "yAxis": [ {
                "minInterval": 1024 * 1024,
                "interval": 1024 * 1024 * 1024,
                "formatter": "binary_byte"
             }
         ],
         "metrics":[{
             "name": "heap",
         },{
            "name": "heapUsed"
        },{
             "name": "heapCommitted"
         }]
     }]
}
