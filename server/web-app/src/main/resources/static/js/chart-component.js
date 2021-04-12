class ChartComponent {

    constructor(option) {
        this.option = $.extend({
            height: '200px'
        }, option);

        this._chartId = option.containerId + '_chart';
        this._card = $('#' + option.containerId).append(
            '    <div class="card card-block">                             ' +
            '        <div class="card-body" style="padding: 0.25em">                    ' +
            '            <div class="card-chart"></div> ' +
            '        </div>                                     ' +
            '    </div>                                         ')
            .find('.card');
        $(this._card).find('.card-chart').attr('id', this._chartId).height(this.option.height);

        this._chart = echarts.init(document.getElementById(this._chartId));
        window.addEventListener("resize", () => {
            this._chart.resize();
        });
    }

    header(text) {
        if (this._header == null) {
            var rnd = Math.random();
            this._header = $(this._card).prepend(
                '<div class="card-header d-flex" style="padding: 0.5em 1em">' +
                '<span class="header-text btn-sm"></span>' +
                //            '<div id="intervalSelector" class="dropdown ml-auto">                                                                                         ' +
                //            '<button class="btn btn-sm"><span class="far fa-bell"></span></button>' +
                //            '    <button class="btn btn-sm dropdown-toggle" id="dropdownMenuButton-"' + rnd + ' type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"> ' +
                //            '        Time Interval                                                                                                                ' +
                //            '    </button>                                                                                                                        ' +
                //            '    <div class="dropdown-menu" aria-labelledby="dropdownMenuButton-' + rnd + '">                                                                 ' +
                //            '        <a class="dropdown-item" href="#">5 min</a>                                                                                  ' +
                //            '        <a class="dropdown-item" href="#">15 min</a>                                                                                 ' +
                //            '        <a class="dropdown-item" href="#">30 min</a>                                                                                 ' +
                //            '        <a class="dropdown-item" href="#">1 hour</a>                                                                                 ' +
                //            '        <a class="dropdown-item" href="#">3 hour</a>                                                                                 ' +
                //            '        <a class="dropdown-item" href="#">6 hour</a>                                                                                 ' +
                //            '        <a class="dropdown-item" href="#">12 hour</a>                                                                                ' +
                //            '        <a class="dropdown-item" href="#">24 hour</a>                                                                                ' +
                //            '        <a class="dropdown-item" href="#">Today</a>                                                                                  ' +
                //            '    </div>      ' +
                //            '</div></div>          ' +
                '</div>').find('.header-text');
        }
        $(this._header).html(text);
        return this;
    }

    title(text) {
        if (this._title == null) {
            this._title = $(this._card).find('.card-body').prepend('<h5 class="card-title">Card title</h5>').find('.card-title');
        }
        $(this._title).html(text);
        return this;
    }

    getOption() {
        return this.option;
    }

    load(option) {
        option = $.extend({
            ajaxType: 'POST',
            processResult: function (data) {
                return data;
            }
        }, option);

        this._chart.showLoading({text: 'Loading...'});

        $.ajax({
            type: option.ajaxType,
            url: option.url,
            async: true,
            data: option.ajaxData,
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                this._chart.hideLoading();
                //this.showLines(option, option.processResult(data));
                this._chart.setOption(option.processResult(data));
            },
            error: (data) => {
                this._chart.hideLoading();
                console.log(data);
            }
        });
    }

    /**
     *
     */
    showLines(option) {
        const charOption = {
            title: {
                text: option.title,
                left: "center",
                textStyle: {
                    fontSize: 14
                }
            },
            color: ['#0098d9', '#90c31d'],
            tooltip: {
                trigger: 'axis'
            },
            grid: [{
                right: '4%',
                left: '4%',
                containLabel: true
            }],
            xAxis: [
                {
                    type: 'category',
                    axisTick: {
                        alignWithLabel: true
                    },
                    gridIndex: 0,
                    data: option.xLabels
                }
            ],
            yAxis: [
                {
                    type: 'value',
                    name: option.yLabel,
                    position: 'left',
                }
            ],
            series: [
                {
                    name: option.yLabel,
                    type: 'line',
                    data: option.data,
                }
            ]
        };
        this._chart.setOption(charOption);
    }

    setChartOption(chartOption) {
        this._chart.setOption(chartOption);
        return this;
    }

    getChartOption() {
        return this._chart.getOption();
    }

    showLoading() {
        this._chart.showLoading();
    }

    hideLoading() {
        this._chart.hideLoading();
    }

    resize() {
        this._chart.resize();
    }
}