class ChartComponent {

    constructor(option) {
        this._selectionHandler = null;
        this._selectionClearHandler = null;
        this._openHandler = null;

        this._selectionState = false;

        this.option = $.extend({
            height: '200px',
            showLegend: true
        }, option);

        this._chartId = option.containerId + '_chart';
        this._card = $('#' + option.containerId).append(
            '    <div class="card card-block" style="border-radius: .0em !important;">                             ' +
            '        <div class="card-body" style="padding: 0.25em">                    ' +
            '            <div class="card-chart"></div> ' +
            '        </div>                                     ' +
            '    </div>                                         ')
            .find('.card');
        $(this._card).find('.card-chart').attr('id', this._chartId).height(this.option.height);

        this._chart = echarts.init(document.getElementById(this._chartId));
        this._chart.on('brushEnd', (params) => {
            if (this._selectionHandler == null) {
                return;
            }
            if (params.areas.length === 0) {
                return;
            }

            const rangeIndex = params.areas[0].coordRange;
            const startIndex = rangeIndex[0];
            const endIndex = rangeIndex[1];
            if (startIndex === endIndex) {
                return;
            }
            this._selectionHandler(this._chart.getOption(), startIndex, endIndex);
        });

        window.addEventListener("resize", () => {
            this._chart.resize();
        });
    }

    getUIContainer() {
        return this._card;
    }

    header(text) {
        let headerText = $(this._card).find('.header-text');
        if (headerText.length === 0) {
            const header = $(this._card).prepend(
                '<div class="card-header d-flex" style="padding: 0.5em 1em">' +
                '<span class="header-text btn-sm"></span>' +
                '<div class="tools ml-auto">' +
                '<button style="display:none" class="btn btn-sm btn-select"><span class="far fa-object-ungroup" title="selection"></span></button>' +
                '<button style="display:none" class="btn btn-sm btn-open"><span class="far fa-window-maximize" title="open"></span></button>' +
                //'    <button class="btn btn-sm btn-alert"><span class="far fa-bell" title="alert"></span></button>' +
                '</div>' +
                '</div>');

            headerText = header.find('.header-text');
        }
        $(headerText).html(text);
        return this;
    }

    height(height) {
        $(this._card).find('.card-chart').height(height);
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

    /**
     * {
     *     ajaxType: 'POST',
     *     processResult: function (data){
     *         return echarts_option;
     *     },
     *     url: url,
     *     ajaxData: {},
     *     operation: merge | replace
     * }
     */
    load(option) {
        //
        // clear the selection
        //
        if (this._selectionHandler != null) {
            this.#clearRangeSelection();
        }

        // reload the chart
        option = $.extend({
            ajaxType: 'POST',
            processResult: function (data) {
                return data;
            },
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
                const returnedOption = option.processResult(data);
                if (returnedOption == null) {
                    return;
                }

                // if (returnedOption.series != null) {
                //     //
                //     // merge series
                //     //
                //     $.each(returnedOption.series, (index, s) => {
                //         this._chartSeries[s.name] = s;
                //     });
                //     const series = [];
                //     for (const name in this._chartSeries) {
                //         series.push(this._chartSeries[name]);
                //     }
                //     returnedOption.series = series;
                // }
                const isReplace = returnedOption.replace !== undefined && returnedOption.replace;
                if (isReplace || (this.option.showLegend && !this.hasUserSelection())) {
                    let legend = {
                        id: 'l',
                        data: [],
                        selected: {}
                    };
                    returnedOption.series.forEach(s => {
                        legend.data.push({
                            name: s.name,
                            icon: 'circle'
                        });
                        legend.selected[s.name] = s.selected;
                    });
                    returnedOption.legend = legend;
                }
                if (isReplace) {
                    this._chart.setOption(returnedOption, {replaceMerge: ['series', 'legend']});
                } else {
                    this._chart.setOption(returnedOption);
                }
            },
            error: (data) => {
                this._chart.hideLoading();
                console.log(data);
            }
        });
    }

    clearLines(name) {
        const newSeries = [];
        const currentOption = this.getChartOption();
        if (name != null) {
            $.each(currentOption.series, (index, s) => {
                if (!s.name.startsWith(name)) {
                    newSeries.push(s);
                }
            });
        }
        currentOption.series = newSeries;
        this._chart.setOption(currentOption, true);
    }

    containsLine(name) {
        const currentOption = this.getChartOption();
        for (const s in currentOption.series) {
            if (s.name === name) {
                return true;
            }
        }
        return false;
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

    dispose() {
        this._chart.dispose();
    }

    setOpenHandler(openHandler) {
        if ($(this._card).find('btn-open').length === 0) {
            const ctrl = $(this._card).find('.tools');
            ctrl.find(".btn-open").css('display', '').click(() => {
                if (this._openHandler != null)
                    this._openHandler.apply();
            });
        }

        this._openHandler = openHandler;
        return this;
    }

    setClickHandler(handler) {
        this._chart.on('click', (e) => {
            handler(e);
        });
        return this;
    }

    setSelectionHandler(handler, clearHandler) {
        const ctrl = $(this._card).find('.tools')
        ctrl.find(".btn-select").css('display', '').click(() => {
            this._chart.dispatchAction({
                type: 'takeGlobalCursor',
                key: 'brush',
                brushOption: {
                    brushType: this._selectionState ? false : 'lineX',
                    brushMode: 'single'
                }
            });
            const btn = $(this._card).find('.tools').find('.btn-select');
            if (this._selectionState) {
                btn.removeClass('btn-primary');
                this.#clearRangeSelection();
            } else {
                btn.addClass('btn-primary');
            }
            this._selectionState = !this._selectionState;
        });

        this._selectionHandler = handler;
        this._selectionClearHandler = clearHandler;
        return this;
    }

    //PRIVATE
    hasUserSelection() {
        const chartOption = this.getChartOption();
        if (chartOption === undefined) {
            return false;
        }
        const oldLegend = chartOption.legend;
        if (oldLegend === undefined || oldLegend.length === 0) {
            return false;
        }
        if (oldLegend[0].selected === undefined)
            return false;

        for (const prop in oldLegend[0].selected) {
            // the 'selected' object has a property
            return true;
        }
        return false;
    }

    #clearRangeSelection() {
        this._chart.dispatchAction({
            type: 'brush',
            command: 'clear',
            areas: [],
        });

        // call clear handler
        if (this._selectionClearHandler != null) {
            this._selectionClearHandler();
        }
    }
}