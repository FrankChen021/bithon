This directory holds modules that provides querying capabilities to both internal and internal storages.

Supported query:
- JSON-based query which is friendly to FE
- Metric Query Language(mql) which is a Prometheus-like query which is friendly to people

JSON Query:

## Internal Implementation

Metric Query Language:

```mermaid
graph LR;
    MQL[Query Text] --> AST[MetricExpression AST]
    AST --> QueryPipeline[Query Pipelines]
    QueryPipeline --> Step1[Query Step 1]
    QueryPipeline --> Step2[Query Step 2]
    QueryPipeline --> StepN[Query Step N]
    Step1 --> StepM
    Step2 --> StepM
    StepN --> StepM
    StepM --> Result[Result]
```

For each step, it can be an arithmetic operation, a function call, or a subquery. 
The result of each step is passed to the next step until the final result is obtained.

If a step is a sub query, it's a JSON-based query, which is also a pipeline:

```mermaid
graph LR;
    Query[JSON Query] ---> SelectStatement
    SelectStatement ---> Select ---> SlidingWindowAggregation ---> Result
```

- SlidingWindowAggregation is optional
