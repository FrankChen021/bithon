
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
  },
  legend: {
    type: 'scroll',
    top: 0,
    data: [
      { name: 'Active Threads', icon: 'circle' },
      { name: 'Connection Count', icon: 'circle' },
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
    }
  ],
  series: [
    {
      name: 'Active Threads',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#f759ab',
      lineStyle: { width: 1, color: '#f759ab' },
      itemStyle: { color: '#f759ab', opacity: 0 },
    },
    {
      name: 'Connection Count',
      type: 'line',
      areaStyle: { opacity: 0.3 },
      data: [],
      color: '#9254de',
      lineStyle: { width: 1, color: '#9254de' },
      itemStyle: { color: '#9254de', opacity: 0 },
    }
  ],
};
