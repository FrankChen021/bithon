class SearchBar {
    constructor() {
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
            '            <form class="mx-2 my-auto d-inline" style="width: 40%">\n' +
            '                <div class="input-group">\n' +
            '                    <input type="text" class="form-control" placeholder="trace id/query id">\n' +
            '                    <span class="input-group-append">\n' +
            '                        <button class="btn btn-outline-secondary" type="button">Search</button>\n' +
            '                    </span>\n' +
            '                </div>\n' +
            '            </form>\n' +
            '        </div>');
        const input = navbar.find("input");
        navbar.find("button").click(() => SearchBar.#search(input.val().trim()));

        const uri = decodeURI(window.location.href);
        const paramPos = uri.indexOf('?') + 1;
        if (paramPos > 0) {
            const idPos = uri.indexOf("id=", paramPos);
            const nextParamPos = uri.indexOf("&", idPos);

            if (nextParamPos !== -1) {
                this._id = uri.substring(idPos + 3, nextParamPos);
            } else {
                this._id = uri.substring(idPos + 3);
            }
            input.val(this._id);
        } else {
            this._id = "";
        }
    }

    #search(id) {
        if (id === '') {
            return;
        }
        if (id === this._id) {
            return;
        }
        window.location.href = `/web/trace/detail?id=${id}&type=auto`;
    }
}
