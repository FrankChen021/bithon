
class AppSelector {

    constructor(appName) {
        this._listeners = [];
        this._control = $('<select class="form-control"></select>');

        new MetadataClient(apiHost).getApplications((appList)=>{
            $.each(appList, (index, app) => {
                if ( appName === app.applicationName) {
                    $(this._control).append(`<option selected="true">${app.applicationName}</option>`);
                } else {
                    $(this._control).append(`<option>${app.applicationName}</option>`);
                }
            });
        }, ()=>{});

        this._control.change((e)=>{
            const text = e.target.selectedOptions[0].text;
            const value = e.target.selectedOptions[0].value;
            $.each(this._listeners, (index, listener)=>{
                listener(text, value);
            });
        });
    }

    childOf(id) {
        $('#'+id).append(this._control);
        return this;
    }

    registerAppChangedListener(listener) {
        this._listeners.push(listener);
        return this;
    }
}