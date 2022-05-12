class ChartComponent {

    constructor(option) {
        this._selectedHandler = null;
        this._selectionHideHandler = null;
        this._selectionShowHandler = null;
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
            if (this._selectedHandler == null) {
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
            this._selectedHandler(this._chart.getOption(), startIndex, endIndex);
        });

        window.addEventListener("resize", () => {
            this._chart.resize();
        });
    }

    getUIContainer() {
        return this._card;
    }

    header(text) {
        let headerText = this._card.find('.header-text');
        if (headerText.length === 0) {
            const header = this._card.prepend(
                '<div class="card-header d-flex" style="padding: 0.5em 1em">' +
                '<span class="header-text btn-sm"></span>' +
                '<div class="tools ml-auto">' +
                '<button class="btn btn-sm btn-select" style="display:none" ><span class="far fa-object-ungroup" title="selection"></span></button>' +
                '<button class="btn btn-sm btn-hide"><span class="far fa-window-minimize" title="hide"></span></button>' +
                '<button class="btn btn-sm btn-open" style="display:none" ><span class="far fa-window-maximize" title="open"></span></button>' +
                //'    <button class="btn btn-sm btn-alert"><span class="far fa-bell" title="alert"></span></button>' +
                '</div>' +
                '</div>');
            header.find('.btn-hide').click(() => {
                this._card.find('.card-body').toggle();
            });

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
        if (this._selectedHandler != null) {
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

                const refreshMode = returnedOption.refreshMode === undefined ? 'refresh' : returnedOption.refreshMode;
                switch(refreshMode) {
                    case 'add':
                    {
                        //
                        // merge series
                        //
                        const oldOption = this.getChartOption();
                        if (oldOption.series !== undefined) {
                            const seriesMap = {};
                            $.each(oldOption.series, (index, s) => {
                                seriesMap[s.name] = s;
                            });
                            $.each(returnedOption.series, (index, s) => {
                                seriesMap[s.name] = s;
                            });

                            const newSeries = [];
                            $.each(seriesMap, (name, s) => {
                                newSeries.push(s);
                            });
                            returnedOption.series = newSeries;
                        }

                        //
                        //merge legend
                        //
                        $.each(returnedOption.series, (index, s) => {
                            let exist = false;
                            for (let i = 0; i < oldOption.legend[0].data.length; i++) {
                                if (oldOption.legend[0].data[i].name === s.name) {
                                    exist = true;
                                }
                            }
                            if (!exist) {
                                oldOption.legend[0].data.push({
                                    name: s.name,
                                    icon: 'circle'
                                });
                                oldOption.legend[0].selected[s.name] = s.selected;
                            }
                        });
                        returnedOption.legend = oldOption.legend[0];
                        this._chart.setOption(returnedOption);
                    }
                        break;
                    case 'refresh':
                        if (this.option.showLegend && !this.hasUserSelection()) {
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
                        this._chart.setOption(returnedOption);
                        break;
                    case 'replace':
                    {
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
                        this._chart.setOption(returnedOption, {replaceMerge: ['series', 'legend']});
                    }
                        break;
                    default:
                        console.log(`unknown refresh mode ${returnedOption.refreshMode}`);
                        break;
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

    setSelectionHandler(selectedHandler, showHandler, hideHandler, clearHandler) {
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
                this.#hideRangeSelection();
            } else {
                btn.addClass('btn-primary');
                this.#showRangeSelection();
            }
            this._selectionState = !this._selectionState;
        });

        this._selectedHandler = selectedHandler;
        this._selectionShowHandler = showHandler;
        this._selectionHideHandler = hideHandler;
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

    #showRangeSelection() {
        if (this._selectionShowHandler != null) {
            this._selectionShowHandler();
        }
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

    #hideRangeSelection() {
        this._chart.dispatchAction({
            type: 'brush',
            command: 'clear',
            areas: [],
        });

        // call clear handler
        if (this._selectionHideHandler != null) {
            this._selectionHideHandler();
        }
    }
}