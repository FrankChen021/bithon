var jdbcPoolMetricsDashboard = {
    "title": "",
    "charts":[{
        "dataSource": "jdbc-pool-metrics",
        "title": "JDBC Connection Pool",
        "width": 4, //1,2,3,4
        "metrics":[{
            "name": "activeCount"
        },{
            "name": "createCount",
        }]
    },{
        "dataSource": "jdbc-pool-metrics",
        "title": "Transaction",
        "width": 4, //1,2,3,4
        "metrics":[{
          "name": "executeCount"
        },{
           "name": "commitCount"
        },{
           "name": "rollbackCount"
        }]
     }]
}
