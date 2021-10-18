
# Setup hooks to check code style before push

```bash
sh setup-hooks.sh
```
# Add License Header for new files

All source code files require license header.

You should either copy the license from licenses/template.txt to the newly added source file,
or execute the following command to add license header automatically.

```bash
mvn com.mycila:license-maven-plugin:format
```