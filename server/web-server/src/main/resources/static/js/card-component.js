

class CardComponent {
    constructor(containerId) {
        this._id = containerId + '_cardComponent';
        this._card = $('#'+containerId).append('    <div class="card card-block"> ' +
                                               '        <div class="card-body">   ' +
                                               '        </div>                    ' +
                                               '    </div>                        '
        ).find('.card').attr('id', this._id);
    }

    header(text) {
        if ( this._header == null ) {
            this._header = $(this._card).prepend('<div class="card-header"></div>').find('.card-header');
        }
        $(this._header).html(text);
        return this;
    }

    title(text) {
        if ( this._title == null ) {
            this._title = $(this._card).find('.card-body').prepend('<h5 class="card-title">Card title</h5>').find('.card-title');
        }
        $(this._title).html(text);
        return this;
    }
}