
const optionOfCpuTimeAndLoad = {
  title: {
    text: '',
    subtext: '',
    show: false,
    textStyle: {
      fontSize: 13,
      fontWeight: 'normal',
    },
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'line',
      label: {
        backgroundColor: '#283b56',
      },
    },
    formatter: params => {
      let result = (params[0] || params).axisValue;
      params.forEach(p => {
        let s = '';
        switch (p.seriesName) {
//          case '处理时间':
//            s = `${Math.round(p.data / 10000) / 100} ms`;
//            break;
          case 'Usage':
            s = `${Math.round(p.data * 100) / 100} %`;
            break;
          default:
            break;
        }
        result += `<br />${p.marker}${p.seriesName}: ${s}`;
      });
      return result;
    },
  },
  legend: {
    type: 'scroll',
    top: 0,
    data: [
      { name: 'Usage', icon: 'circle' },
    ],
  },
  dataZoom: {
    show: false,
    start: 0,
    end: 100,
  },
  grid: {
    left: 40,
    right: 40,
    bottom: 20,
    top: 40,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    axisLabel: {},
    data: [],
  },
  yAxis: [
    {
      type: 'value',
      min: 0,
      scale: true,
      splitLine: { show: true },
      axisLine: { show: false },
      axisTick: {
        show: false,
      },
      axisLabel: {
        formatter: value => `${Math.round(value * 100) / 100} %`,
      },
    }
  ],
  series: [
    {
      name: 'Usage',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#3ba1ff',
      lineStyle: { width: 1, color: '#3ba1ff' },
      itemStyle: { color: '#3ba1ff', opacity: 0 },
    },
  ],
};

const heapMemoryOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'line',
        label: {
          backgroundColor: '#283b56',
        },
      },
      formatter: params => {
        let result = (params[0] || params).axisValue;
        params.forEach(p => {
          const s = binaryByteFormat(p.data);
          result += `<br />${p.marker}${p.seriesName}: ${s}`;
        });
        return result;
      },
    },
    dataZoom: {
        show: false,
        start: 0,
        end: 100,
      },
      grid: {
        left: 60,
        right: 0,
        top: 40,
        bottom: 20,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        //axisLabel: {
        //  formatter: axisTimeFormatter,
        //},
        data: [],
      },
      yAxis: {
        type: 'value',
        min: 0,
        scale: true,
        name: '',
        splitLine: {
          lineStyle: { type: 'dotted' },
        },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          formatter: binaryByteFormat,
        },
        minInterval: 1024 * 1024,
        interval: 1024 * 1024 * 1024,
      },
    series: [
        {
          name: 'Heap',
          type: 'line',
          areaStyle: { opacity: 0.3 },
          data: [],
          color: '#f759ab',
          lineStyle: { width: 1, color: '#f759ab' },
          itemStyle: { color: '#f759ab', opacity: 0 },
        },
        {
          name: 'Used',
          type: 'line',
          areaStyle: { opacity: 0.3 },
          data: [],
          color: '#9254de',
          lineStyle: { width: 1, color: '#9254de' },
          itemStyle: { color: '#9254de', opacity: 0 },
        }
    ]
}

const threadsOption = {
  title: {
    text: '',
    subtext: '',
    show: false,
    textStyle: {
      fontSize: 13,
      fontWeight: 'normal',
    },
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'line',
      label: {
        backgroundColor: '#283b56',
      },
    },
    formatter: params => {
      let result = (params[0] || params).axisValue;
      params.forEach(p => {
        result += `<br />${p.marker}${p.seriesName}: ${compactFormat(p.data)}`;
      });
      return result;
    },
  },
  legend: {
    type: 'scroll',
    top: 0,
    data: [
      { name: 'Peak', icon: 'circle' },
      { name: 'Daemon', icon: 'circle' },
      { name: 'Active', icon: 'circle' },
    ],
  },
  grid: {
    left: 40,
    right: 0,
    bottom: 20,
    top: 40
  },
    dataZoom: {
      show: false,
      start: 0,
      end: 100,
    },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    axisLabel: {},
    data: [],
  },
  yAxis: {
    type: 'value',
    min: 0,
    splitLine: {
      lineStyle: { type: 'dotted' },
    },
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { formatter: compactFormat },
  },
  series: [
    {
      name: 'Peak',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      lineStyle: { width: 1, color: '#f759ab' },
      itemStyle: { color: '#f759ab', opacity: 0 },
    },
    {
      name: 'Daemon',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#b2e3f8',
      lineStyle: { width: 1, color: '#9254de' },
      itemStyle: { color: '#9254de', opacity: 0 },
    },
    {
      name: 'Active',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#b2e3f8',
      lineStyle: { width: 1, color: '#bae637' },
      itemStyle: { color: '#bae637', opacity: 0 },
    },
  ],
};