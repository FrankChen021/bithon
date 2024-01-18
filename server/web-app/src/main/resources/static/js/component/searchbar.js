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

        //
        // Binding input to view and model
        //
        if (window.queryParams !== undefined && window.queryParams["id"] != null) {
            this._mInputId = window.queryParams["id"];
            input.val(this._mInputId);
        }
    }

    #search(id) {
        if (id === '') {
            return;
        }
        if (id === this._mInputId) {
            // no change
            return;
        }

        const uri = `/web/trace/detail?id=${id}&type=auto`;
        if (this._mOpenNewWindow) {
            window.open(uri);
        } else {
            window.location.href = uri;
        }
    }
}
