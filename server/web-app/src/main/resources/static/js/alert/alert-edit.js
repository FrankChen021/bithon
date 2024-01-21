function getModelFromUI() {
    if (gEditingAlert.appName === null) {
        showMessage("Please select an application");
        return null;
    }

    if (isBlankString(gEditingAlert.name)) {
        showMessage("Please input the alert name");
        return null;
    }
    if (!isValidNumber(gEditingAlert.detectionLength)) {
        showMessage("Please set the Checking Interval");
        return null;
    }

    //
    // check conditions
    //
    const conditions = [];
    for (const conditionId in gEditingConditions) {
        const condition = gEditingConditions[conditionId];

        if (isBlankString(condition.dataSource)) {
            showMessage(`Condition ${conditionId} has no data source`);
            return null;
        }

        const dimensionList = [];
        for (const dimensionName in condition.dimensions) {
            const dimensionCond = condition.dimensions[dimensionName];

            if (dimensionCond.expected != null && !isBlankString(dimensionCond.expected.value)) {
                dimensionList.push(dimensionCond);
            }
        }

        const metric = condition.metrics[0];
        if (metric.metric == null) {
            showMessage(`Condition ${conditionId} has no metric`);
            return null;
        }
        if (!isValidNumber(metric.expected)) {
            showMessage(`Condition ${conditionId} has invalid threshold`);
            return null;
        }
        try {
            metric.expected = parseInt(metric.expected);
            if (isNaN(metric.expected)) {
                showMessage(`Condition ${conditionId} threshold has non-digits`);
                return null;
            }
        } catch (e) {
            showMessage(`Condition ${conditionId} threshold has non-digits`);
            return null;
        }

        conditions.push({
            id: conditionId,
            dataSource: condition.dataSource,
            dimensions: dimensionList,
            metrics: condition.metrics
        });
    }
    conditions.sort(function (a, b) {
        return a.id.localeCompare(b.id);
    });

    //
    // check triggers
    //
    const triggers = [];
    for (const id in gEditingTriggers.triggers) {
        const trigger = gEditingTriggers.triggers[id];
        if (trigger.expression == null) {
            showMessage("Rules can't be empty");
            return null;
        }
        if (trigger.severity == null) {
            showMessage("Severity can't be empty");
            return null;
        }
        triggers.push(trigger);
    }

    //
    // check notification
    //
    if (!isValidNumber(gEditingNotification.silencePeriod)) {
        showMessage("Checking Interval Can't be empty");
        return;
    }

    //
    // convert to POST object
    //
    const alert = {
        alertId: gEditingAlert.alarmId,
        appName: gEditingAlert.appName,
        disabled: gEditingAlert.disabled,
        name: gEditingAlert.name,
        detectionLength: gEditingAlert.detectionLength,
        expressions: conditions,
        triggers: triggers,
        notificationSpec: {
            "type": "ding",
            "url": gEditingNotification.url,
            "silencePeriod": gEditingNotification.silencePeriod
        }
    };

    const data = JSON.stringify(alert, null, 4);
    console.log(data);
    return data;
}

function create() {
    const data = getModelFromUI();
    if (data == null) {
        return;
    }

    const dlg = new Dialog();
    dlg.showLoadingDialog(null, 'Saving...');

    $.ajax({
        type: "POST",
        url: apiHost + "/alerting/api/alert/create",
        async: true,
        data: data,
        dataType: "json",
        contentType: "application/json",
        success: function (data) {
            const result = data;
            if (data.code !== 200) {
                dlg.showMessage(data.message, "Error");
            } else {
                dlg.showMessage("Successfully create alert");
            }
        },
        error: function (data) {
            console.log(data);
            dlg.showMessage(data.responseJSON.message, "Error");
        }
    });
}


function update() {
    const data = getModelFromUI();
    if (data == null) {
        return;
    }

    const dlg = new Dialog();
    dlg.showLoadingDialog(null, 'Saving...');

    $.ajax({
        type: "POST",
        url: "/alerting/api/alert/update",
        async: true,
        data: data,
        dataType: "json",
        contentType: "application/json",
        success: function (data) {
            const result = data;
            if (data.code !== 200) {
                dlg.showMessage(data.message, "Error");
            } else {
                dlg.showMessage("Successfully modified alert");
            }
        },
        error: function (data) {
            console.log(data);
            dlg.showMessage(data.responseJSON.message, "Error");
        }
    });
}