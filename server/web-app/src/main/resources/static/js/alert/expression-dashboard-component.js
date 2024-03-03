class ExpressionDashboardComponent {

    constructor(siblingId) {
        this.metricComponents = {};
        this.siblingId = siblingId;
    }

    renderExpression(expr) {
        $.ajax({
            type: "POST",
            url: "/api/alerting/alert/parse",
            async: true,
            data: JSON.stringify({
                expression: expr
            }),
            dataType: "json",
            contentType: "application/json",
            success: (data) => {
                if (data.code !== 200) {
                    bootbox.alert({
                        backdrop: true,
                        title: "Error",
                        message: data.message
                    });
                    return;
                }

                const expressions = data.data.expressions;

                //
                // Remove non-existing chart
                //
                const remove = [];
                $.each(this.metricComponents, (id, component) => {
                    if (expressions.find((expr) => expr.id === id) === undefined) {
                        remove.push(id);
                    }
                });
                $.each(remove, (index, id) => {
                    const component = this.metricComponents[id];
                    component.dispose();

                    delete this.metricComponents[id];
                });

                //
                // Render the parsed expression
                //
                $.each(data.data.expressions, (index, expression) => {
                    this.#renderOneExpression(expression);
                });
            },
            error: (data) => {
                let message = '';
                if (data.responseJSON == null) {
                    message = data.responseText
                } else {
                    $.each(data.responseJSON, (p, v) => {
                        if (v !== null) {
                            message += '<b>' + p + '</b>';
                            message += ': ';
                            message += v;
                            message += '\n';
                        }
                    });
                }

                bootbox.alert({
                    backdrop: true,
                    title: "Error",
                    message: '<pre>' + message + '</pre>'
                });
            }
        });
    }

    #renderOneExpression(expression) {
        const expressionId = expression.id;

        let window = parseInt(expression.window.substring(0, expression.window.length - 1));
        if (expression.window.endsWith('m')) {
        } else if (expression.window.endsWith('h')) {
            // Turn into minute
            window = window * 60;
        }

        //
        const end = new Date().truncate(window * 60000).getTime();
        const start = end - window * 60 * 1000;

        const chartOption = {
            componentId: 'chart-' + expressionId,
            class: '',
            title: expression.expressionText,

            dataSourceName: expression.from,
            filterExpression: expression.where,
            metric: expression.select,
            window: window,
            start: start,
            end: end + 5 * 60 * 1000,
            hours: 1,
            range: {
                start: start,
                end: end
            },
            threshold: expression.alertExpected
        };

        if (this.metricComponents[expressionId] === undefined) {
            this.metricComponents[expressionId] = new MetricComponent();
        }
        this.metricComponents[expressionId].showAfter($('#' + this.siblingId), chartOption);
    }
}