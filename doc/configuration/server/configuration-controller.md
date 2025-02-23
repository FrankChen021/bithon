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
        enabled: true
        # The following define a global rule to ALLOW any operation from any user.
        # Refer to the PermissionConfig to know more about the configuration
        rbac:
          users:
            - name: "*"
              permissions:
                - operation: RW
                  resource: "*"
                  application: "*"
   web:
    security:
      jwtTokenSignKey:
      jwtTokenValiditySeconds:
```

## Permission Control

To protect the target application from unauthorized access, the controller provides a permission control mechanism. The permission control is based on the RBAC model. 
When the `bithon.agent-controller.permission.enabled` is set to `true`, the controller will check the permission for each request.
To know more about the configuration, see the `org.bithon.server.agent.controller.rbac.Permission` class.

And also, `bithon.web.security.jwtTokenSignKey`
and `bithon.web.security.jwtTokenValiditySeconds` MUST be configured to the SAME configuration
as they're configured at the web server module.
See the `JwtConfig` to know more about it.
