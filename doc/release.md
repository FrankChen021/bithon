# Step 1. Configure maven settings.xml
```xml
<server>
  <id>sonatype-nexus</id>
  <username>FrankChen021</username>
  <password>password</password>
</server>
```

# Step2. make sure there is GPG private keys on your server

If there is no GPG private key, use following command to generate your key
```bash
gpg --full-generate-key 
```

# Step 3. Build and deploy to sonatype

```bash
export GPG_TTY=$(tty)
mvn clean deploy
```