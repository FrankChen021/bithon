
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

| mapping type | args |
| --- | --- |
| uri | array of parameter names that are used to extract user id from the uri |
| name | name of the name-value pair in trace span's `tag` property |

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

| user id | trace id |
| ---- | --- |
| abcd | aabbccddeeff |
| cdef | aabbccddeeff |

So, you can search the trace either by given `abcd` or `cdef` or `aabbccddeeff` to get the trace logs.