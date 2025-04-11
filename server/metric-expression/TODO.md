## Must to be done
- [x] In MetricQueryApi, convert ColumnarResponse to QueryResponse
- [x] For base line comparison, returns the base/delta columns
- [ ] Fix the Grammar to move predicate after arithmetic expression
- [x] Define Column structure
- [x] Change ColumnarResponse to Table
 
## Optimization
- [ ] Push the literal computation to the storage layer
- [ ] Push the metrics in the same data source and with same GROUP-BY/Label-Selector to the storage layer
- [x] Use [] instead of List on the ColumnarResponse