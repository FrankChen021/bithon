const gEditingTriggers = {
    triggerId: 1,
    triggers: {}
};

class CompositeRuleComponent {
    /**
     * {
     *     container: container that holds this component
     * }
     * @param option
     */
    constructor(option) {
        this.vContainer = option.container;
    }

    addCompositeRules(rules) {
        $.each(rules, (index, rule) => this.addCompositeRule(rule));
        return this;
    }

    addCompositeRule(rule) {
        this.#createRow();
        this.#updateRow(rule);
    }

    #createRow() {
        const id = "trigger-item-" + gEditingTriggers.triggerId++;

        // MMVM Handling
        gEditingTriggers.triggers[id] = {};

        // create UI
        const rowComponent = $(
            '<div class="input-group trigger-item" style="margin-bottom: 5px;">                    ' +
            '    <div class="input-group-prepend"><span class="input-group-text" style="border-left: 1px solid #ccc;">Rules</span></div>' +
            '    <input type="text" class="form-control trigger-expression"                                 ' +
            '           placeholder="rules" maxlength="200" style="width: 260px"/>                           ' +
            '    <div class="input-group-prepend"><span class="input-group-text" style="border-left: 1px solid #ccc;">Severity</span> </div>' +
            '    <select class="form-control trigger-severity">                       ' +
            '        <option>--Please select--</option>                                                   ' +
            '        <option value="MODERATE">Moderate</option>                                                   ' +
            '        <option value="SEVERE">Severe</option>                                                     ' +
            '        <option value="CRITICAL">Critical</option>                                                   ' +
            '    </select>                                                                                  ' +
            '    <div class="input-group-append">                                                                  ' +
            '        <button class="btn btn-outline-info rule-new-row" title="Create a new rule">New</button>         ' +
            '        <button class="btn btn-outline-info rule-del-row" title="Delete this rule">Del</button>         ' +
            '    </div>                                                                                        ' +
            '</div>                                                                                         '
        ).attr("id", id);

        $(rowComponent).find(".trigger-expression").val('').change(function (e) {
            gEditingTriggers.triggers[id].expression = e.target.value;
        });
        $(rowComponent).find(".trigger-severity").change(function (e) {
            gEditingTriggers.triggers[id].severity = e.target.selectedIndex === 0 ? '' : $(e.target.selectedOptions).val();
        });
        this.vContainer.append($(rowComponent));
    }

    #updateRow(rule) {
        const container = $(".trigger-container");

        const ruleItem = $(container).find(".trigger-item:last");
        $(ruleItem).find(".trigger-expression").val(rule.expression).change();
        $(ruleItem).find(".trigger-severity").find("option[value='" + rule.severity + "']").prop("selected", "selected");
        $(ruleItem).find(".trigger-severity").change();
    }

    remove(_this) {
        const rowComponent = $(_this).parent().parent();
        if ($(".trigger-container").find(".trigger-item").length === 1) {
            return;
        }
        const rowId = $(rowComponent).attr("id");
        $(rowComponent).remove();

        // MMVM Handling
        delete gEditingTriggers.triggers[rowId];
    }
}