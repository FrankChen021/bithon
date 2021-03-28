package com.sbss.bithon.agent.plugin.mongodb.intercetpor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/28 11:43
 */
public class MongoCommand {
    private final String database;
    private final String collection;
    private final String command;

    public String getDatabase() {
        return database;
    }

    public String getCollection() {
        return collection;
    }

    public String getCommand() {
        return command;
    }

    public MongoCommand(String database, String collection, String command) {
        this.database = database;
        this.collection = collection;
        this.command = command;
    }
}
