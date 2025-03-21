echo 'Importing database schema'
mysql -u root < src/main/resources/database.sql
echo 'Importing alerting database schema'
mysql -u root < src/main/resources/database-alerting.sql
echo 'Generating Jooq classes'
mvn jooq-codegen:generate