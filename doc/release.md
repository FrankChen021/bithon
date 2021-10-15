# Step 1. Configure maven settings.xml
```xml
<server>
  <id>sonatype-nexus</id>
  <username>FrankChen021</username>
  <password>password</password>
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

# Step 3. Build and deploy to sonatype

```bash
export GPG_TTY=$(tty)
mvn clean deploy
```

# Step 4.

close the project at [staging repository](https://s01.oss.sonatype.org), and then release it if it passes all the check.