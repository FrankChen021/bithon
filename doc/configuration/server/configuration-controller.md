# Controller

The controller is responsible for manage all agents running in client side applications.
1. It responses agent configuration request from the agent side.
2. It calls agent services for profiling.


## Configuration

```yaml
bithon:
   agent-controller:
      enabled: true
      port: 9899
      permission:
         rules:
            - application:
                 type: startwith
                 pattern: bithon-
              authorization: ["525", "frankchen@apache.org"]
```

## Permission Control

The `permission` controls how a UPDATE command is authenticated. 

- `application`

  A matcher that how correspond authorization is applied if a given application matches this matcher.
For more information of application matcher, see the source code [IMatcher.java](../../../server/server-commons/src/main/java/org/bithon/server/commons/matcher/IMatcher.java).

- `authorization`

  A list of authorizations, either a token or user name if current deployment is under OAuth2 authentication.

