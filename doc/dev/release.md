The artifacts are published to https://central.sonatype.com/ (FrankChen021)

# Step 1. Configure authentication in settings.xml

Goto https://central.sonatype.com/ to get token.

```xml
<server>
  <id>central</id>
  <username>YOUR_NAME</username>
  <password>YOUR_PASSWORD</password>
</server>
```

# Step2. make sure there is GPG private keys on your server

## generate a key
If there is no GPG private key, use following command to generate your key
```bash
gpg --full-generate-key 
```

## export public key

```bash
(gpg --list-sigs YOUR_KEY_ID && gpg --armor --export YOUR_KEY_ID) > KEY
```

## upload key

Upload your key to [opengpg](https://keys.openpgp.org/upload/)

# Step 3. Build and deploy agent to sonatype

```bash
export GPG_TTY=$(tty)
mvn clean deploy -DskipTests -Pdist -P-server
```

