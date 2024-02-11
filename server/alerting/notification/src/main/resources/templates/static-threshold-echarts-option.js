{
    "backgroundColor": "transparent",
    "animation": false,
    "color": ["#0098d9"],
    "tooltip": {
        "trigger": "axis"
    },
    "grid": [{
        "right": "4%",
        "left": "4%",
        "containLabel": true
    }],
    "xAxis": [{
        "type": "category",
        "axisTick": {
            "alignWithLabel": true
        },
        "data": %xAxisLabelData%
    }],
    "yAxis": [{
        "type": "value",
        "name": "%yLabel%",
        "position": "left",
        "max": %yMax%,
        "splitLine":{
            "show": true,
            "lineStyle": {
                "color": ["#aaa"]
            }
        }
    }],
    "series": {
        "name": "%seriesName%",
        "type": "line",
        "data": %SeriesData%,
        "markLine": {
          "animation": false,
          "symbol": "none",
          "label": {
              "position": "end"
          },
          "data": [{
             "xAxis": "%startPoint%"
           },{
             "xAxis": "%endPoint%"
           },{
            "silent": false,
            "lineStyle": {
               "type":"solid",
               "color":"red"
            },
            "label":{
               "position":"middle",
               "formatter":"报警线"
            },
            "yAxis": %threshold%
        }]
      }
    }
}