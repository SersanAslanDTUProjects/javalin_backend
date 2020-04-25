package database;


import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import java.util.Arrays;

public class DataSource {
    private final static String HOST = "18.185.121.182";
    private final static int PORT = 27017;
    private final static String DATABASE_NAME = "cphPlaygroundsDB";
    private static DB database;
    private static MongoClient mongoClient;

    private final static String user = "myAdmin"; // the user name
    private final static String adminDatabase = "admin"; // the name of the database in which the user is defined
    private final static char[] password = ("njl_nykode").toCharArray(); // the setPassword as a character array

    private DataSource() {
    }

    public static DB getDB() {
        if (database == null) {
            MongoCredential credential = MongoCredential.createCredential(user, adminDatabase, password);
            mongoClient = new MongoClient(new ServerAddress(HOST, PORT),
                    Arrays.asList(credential));
            database = mongoClient.getDB(DATABASE_NAME);
        }
        return database;
    }

    public static MongoClient getClient() {
        if (mongoClient == null) {
            MongoCredential credential = MongoCredential.createCredential(user, adminDatabase, password);
            mongoClient = new MongoClient(new ServerAddress(HOST, PORT),
                    Arrays.asList(credential));
        }
        return mongoClient;
    }

/*  public static DB getDB(){
        if (database == null)
            database = getClient().getDB("test");

        return database;
  }*/

 /* public static MongoClient getClient(){
      if (mongoClient == null)
          mongoClient = new MongoClient(new MongoClientURI("mongodb+srv://s175565:qwe123@todoapp-cn8eq.mongodb.net/test"));

      return mongoClient;
  }

  */

}
