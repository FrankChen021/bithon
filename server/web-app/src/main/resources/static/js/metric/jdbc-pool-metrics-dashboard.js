var jdbc_pool_metrics_dashboard = {
    "title": "",
    "charts":[{
        "dataSource": "jdbc-pool-metrics",
        "title": "JDBC Connection Pool",
        "width": 4, //1,2,3,4
        "metrics":[{
            "name": "activeCount"
        },{
            "name": "poolingCount",
        },{
            "name": "createCount",
        },{
            "name": "destroyCount",
        },{
            "name": "waitThreadCount",
        }]
    },{
        "dataSource": "jdbc-pool-metrics",
        "title": "Transaction",
        "width": 4, //1,2,3,4
        "metrics":[{
          "name": "executeCount"
        },{
            "name": "startTransactionCount"
        },{
           "name": "commitCount"
        },{
           "name": "rollbackCount"
        }]
     }]
}
