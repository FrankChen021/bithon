function showMessage(message, title) {
    bootbox.alert({
        title: title === undefined ? "Info" : title,
        message: message,
        backdrop: true
    });
}

class Dialog {

    constructor() {
        this._dialog = null;
    }

    showLoadingDialog(title, message) {
        this._dialog = bootbox.dialog({
            title: title,
            message: message,
            closeButton: false
        });
    }

    showMessage(message, title, callback) {
        swal({
            title: title,
            text: message
        }).then((value) => {
            if (callback != null) {
                callback();
            }
        })
    }

    closeDialog() {
        this._dialog.modal('hide');
    }
}