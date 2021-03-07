
class AppSelector {

    constructor(apiHost) {
        this._apiHost = apiHost;
    }

    // PUBLIC
    before(id) {
        this._control = $('<select class="form-control"></select>');

        $('#'+id).before(this._control);

        new MetadataClient(this._apiHost).getApplications((appList)=>{
            $.each(appList, (index, app) => {
                $(this._control).append(`<option>${app.name}</option>`);
            });
        }, ()=>{});

        return this;
    }

    childOf(id) {
        this._control = $('<select class="form-control" id="appSelector"></select>');

        $('#'+id).append(this._control);

        new MetadataClient(this._apiHost).getApplications((appList)=>{
            $.each(appList, (index, app) => {
                $(this._control).append(`<option>${app.name}</option>`);
            });
        }, ()=>{});

        return this;
    }
}