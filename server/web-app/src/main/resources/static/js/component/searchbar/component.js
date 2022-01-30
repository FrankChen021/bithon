class SearchBar {
    constructor(openNewWindow) {
        // Model
        this._mInputId = "";
        this._mInputInterval = "";
        this._mOpenNewWindow = openNewWindow;

        // View
        const navbar = $(".navbar").first();
        navbar.append(
            '        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"\n' +
            '                aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">\n' +
            '            <span class="navbar-toggler-icon"></span>\n' +
            '        </button>\n' +
            '        <div class="collapse navbar-collapse" id="navbarSupportedContent">\n' +
            '            <ul class="navbar-nav mr-auto">\n' +
            '                <!-- a placeholder that ensure the search form is aligned at right side -->\n' +
            '            </ul>\n' +
            '            <form class="d-inline" style="width: 40%">\n' +
            '                <div class="input-group">\n' +
            '                    <input type="text" class="form-control" placeholder="trace id/query id">\n' +
            '                    <div class="col-sm-3" style="padding: 0">' +
            '                       <select class="form-control">' +
            '                          <option selected>Today</option>' +
            '                          <option>Yesterday</option>' +
            '                          <option>This Week</option>' +
            '                          <option>Last Week</option>' +
            '                          <option>All</option>' +
            '                       </select>' +
            '                    </div>' +
            '                    <span class="input-group-append">\n' +
            '                        <button class="btn btn-outline-secondary" type="button"><i class=\'fa fa-search\'></i></button>\n' +
            '                    </span>\n' +
            '                </div>\n' +
            '            </form>\n' +
            '        </div>');
        const input = navbar.find("input");
        navbar.find("button").click(() => this.#search(input.val().trim()));
        navbar.find("form").submit((event) => {
                this.#search(input.val().trim());
                event.preventDefault();
            }
        );
        this._vIntervalSelect = navbar.find("select").change((event) => {
            this._timeRange = event.target.selectedIndex;
        });

        //
        // Binding input to view and model
        //
        if (window.queryParams["id"] != null) {
            this._mInputId = window.queryParams["id"];
            input.val(this._mInputId);
        }
    }

    #search(id) {
        if (id === '') {
            return;
        }
        if (id === this._mInputId && this._vIntervalSelect.val() === this._mInputInterval) {
            // no change
            return;
        }

        const interval = this.getInterval();
        const uri = `/web/trace/detail?id=${id}&type=auto&interval=${encodeURI(interval.start + "/" + interval.end)}`;
        if (this._mOpenNewWindow) {
            window.open(uri);
        } else {
            window.location.href = uri;
        }
    }

    getInterval() {
        switch (this._vIntervalSelect.prop('selectedIndex')) {
            case 0://today
                return {
                    start: moment().utc().local().startOf('day').toISOString(),
                    end: moment().utc().local().toISOString()
                }
            case 1://yesterday
                const start = moment().utc().local().startOf('day').subtract(1, 'day');
                return {
                    start: start.toISOString(),
                    end: start.add(1, 'day').toISOString()
                };
            case 2://This Week
                return {
                    start: moment().utc().local().startOf('week').toISOString(),
                    end: moment().utc().local().toISOString()
                };
            case 3://Last Week
            {
                const start = moment().utc().local().startOf('week').subtract(7, 'day');
                return {
                    start: start.toISOString(),
                    end: start.add(7, 'day').toISOString()
                };
            }
            default:
                return {
                    start: "2022-01-01T00:00:00.000Z",
                    end: "2099-12-31T00:00:00.000Z"
                };
        }
    }
}
