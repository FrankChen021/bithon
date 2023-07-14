class Sidebar {
    constructor(containerId) {
        this._container = $('#' + containerId);
    }

    load(folder, rootPath, newWindow) {
        g_DashboardApi.getDashboardList(
            folder,
            (data) => {
                $.each(data, (index, item) => {
                    this.#addDashboardItem({id: item.value, text: item.text}, rootPath, newWindow);
                });
            },
            (data) => {
            });
    }

    select() {
    }

    // PRIVATE
    #addDashboardItem(item, root, newWindow) {
        const i = $(`<a>${item.text}</a>`).click(() => {
            redirect(`${root}/${item.id}`, newWindow);
        });
        this._container.append(i);
    }
}
