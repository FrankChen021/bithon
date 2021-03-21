
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
            var text = e.target.selectedOptions[0].text;
            var value = e.target.selectedOptions[0].value;
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