
## Trace Id mapping

This configuration is used to extract user level transaction id from a trace span 
so that it could be mapped to the trace id of that span.

Once the user leve transaction id is successfully extracted, it will be saved in storage
and user could use this user level id to search corresponding trace.


### Examples

```yaml
bithon:
  tracing:
    mapping:
      - type: uri
        args:
          parameters: ["query_id"]
      - type: name
        args:
          names: ["query_id"]
```

### Explanations

| mapping type | args                                                                   |
|--------------|------------------------------------------------------------------------|
| uri          | array of parameter names that are used to extract user id from the uri |
| name         | name of the name-value pair in trace span's `tag` property             |

Say we have a span log whose `tag` property contains following data:
```json
{
  "uri": "/?queriy_id=abcd&type=insert",
  "query_id": "cdef"
}
```

The configuration above will use the `uri` type mapping to extract 'query_id' parameter from the `uri` name-value pair in the `tag`,
which is `abcd`.

Also the `name` type mapping matches the `query_id` property in the `tag`, and its value, which is `cdef` will be extracted.

Given this trace id of this span is `aabbccddeeff`, following mapping will be saved into the storage.

| user id | trace id     |
|---------|--------------|
| abcd    | aabbccddeeff |
| cdef    | aabbccddeeff |

So, you can search the trace either by given `abcd` or `cdef` or `aabbccddeeff` to get the trace logs.


## Indexes for Span Log Tag

To support fast search on some tags, we need to first define the indexes for the tag names that we want to search on.

### Examples

```yaml
bithon:
  tracing:
    indexes:
      map:
        http.status: 1
        http.uri : 2
```

A map is supported on `bithon.tracing.tagIndexConfig.indexes`.
- key, the tag name in the span log
- val, an integer that is in [1, 16]. And different keys should have different values.

By above configuration, whenever a span log contains `http.status` or `http.uri` in the `tags` property, their values will be extracted from the log and kept as index.
After that, we can search the span log by passing a filter with these two names.