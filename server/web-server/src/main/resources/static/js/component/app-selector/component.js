
class AppSelector {

    constructor() {
        this._listeners = [];
        this._control = $('<select class="form-control"></select>');

        new MetadataClient(apiHost).getApplications((appList)=>{
            $.each(appList, (index, app) => {
                $(this._control).append(`<option>${app.name}</option>`);
            });
        }, ()=>{});

        this._control.change((e)=>{
            $.each(this._listeners, (index, listener)=>{
                listener(e);
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