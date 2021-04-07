var thread_pool_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "thread-pool-metrics",
        "title": "Thread Pool",
        "width": 4, //1,2,3,4
        "metrics":[{
            "name": "activeThreads"
        },{
            "name": "currentPoolSize",
        }]
    },{
        "dataSource": "thread-pool-metrics",
        "title": "Tasks",
        "width": 4, //1,2,3,4
        "metrics":[{
           "name": "totalTaskCount"
        },{
            "name": "successfulTaskCount"
        },{
            "name": "queuedTaskCount"
        },{
            "name": "exceptionTaskCount"
        },{
            "name": "callerRunTaskCount"
        },{
            "name": "abortedTaskCount"
        },{
            "name": "discardedTaskCount"
        },{
            "name": "discardedOldestTaskCount"
         }]
     }]
}
