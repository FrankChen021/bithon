
const optionOfWebServer = {
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
          case 'Requests':
            s = p.data;
            break;
          case 'Cost Time':
            s = p.data + 'ms';
            break;
          default:
            break;
        }
        result += `<br />${p.marker}${p.seriesName}: ${s}`;
      });
      return result;
    }
  },
  legend: {
    type: 'scroll',
    top: 0,
    data: [
      { name: 'Requests', icon: 'circle' },
      { name: 'Cost Time', icon: 'circle' },
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
      minInterval: 1,
      scale: true,
      splitLine: { show: true },
      axisLine: { show: false },
      axisTick: {
        show: false,
      },
      axisLabel: {
      },
    },
    {
      type: 'value',
      min: 0,
      minInterval: 10,
      scale: true,
      splitLine: { show: true },
      axisLine: { show: false },
      axisTick: {
        show: false,
      },
      axisLabel: {
        formatter: value => value + 'ms',
      },
    }
  ],
  series: [
    {
      name: 'Requests',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#f759ab',
      lineStyle: { width: 1, color: '#f759ab' },
      itemStyle: { color: '#f759ab', opacity: 0 },
    },
    {
      name: 'Cost Time',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#9254de',
      lineStyle: { width: 1, color: '#9254de' },
      itemStyle: { color: '#9254de', opacity: 0 },
      yAxisIndex: 1
    }
  ],
};
