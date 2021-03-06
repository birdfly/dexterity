//package com.ht1.dexterity.app;
import java.io.IOException;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoClientURI;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MongoWrapper {
	
	
	
/*
 old start code 
	
public static void main(String[] args) {
    MongoClientURI dbUri = new MongoClientURI("mongodb://tzachi_dar:tzachi_dar@ds053958.mongolab.com:53958/nightscout");
    System.out.println( "Starting");
    try {
        
        MongoClient mongoClient = new MongoClient(dbUri);
        DB db = mongoClient.getDB( "nightscout" );
        
        DBCollection coll = db.getCollection("try1");
        
        coll.createIndex(new BasicDBObject("i", 1));  // create index on "i", ascending

        
        for (int i =0; i < 10; i++) {
            BasicDBObject doc = new BasicDBObject("name", "MongoDB")
            .append("type", "database")
            .append("count", i);
            
            coll.insert(doc);
        }

        
        
        DBCursor cursor = coll.find();
        try {
           while(cursor.hasNext()) {
               System.out.println(cursor.next());
           }
        } finally {
           cursor.close();
        }
        
        System.out.println("Now with index...");
        System.out.println("===================");
        
        DBObject query = new BasicDBObject();
        cursor = coll.find(query);
        cursor.sort(new BasicDBObject("count", -1));
        try {
            while(cursor.hasNext()) {
                System.out.println(cursor.next());
            }
         } finally {
            cursor.close();
         }
        
        // now get the numbers that are bigger than 7
        System.out.println("Now the numbers bigger than 7");
        System.out.println("===================");
        
        query = new BasicDBObject(new BasicDBObject("count", new BasicDBObject("$gt", 7)));
        cursor = coll.find(query);
        cursor.sort(new BasicDBObject("count", 1));
        try {
            while(cursor.hasNext()) {
                System.out.println(cursor.next());
            }
         } finally {
            cursor.close();
         }        
        
        
        
        //        Set<String> colls = db.getCollectionNames();

//        for (String s : colls) {
//            System.out.println(s);
//        }
    } catch (UnknownHostException e) {
        //throw new IOException("Error connecting to mongo host " + dbUri, e);
        System.out.println( "Failed to open table");
    }
      
    }

*/
	
	
	MongoClient mongoClient_;
	String dbUriStr_;
	String dbName_;
	String collection_;
	String index_;
	String machineName_;
	
	public MongoWrapper(String dbUriStr, String collection, String index, String machineName) {
		dbUriStr_ = dbUriStr;
		// dbName is the last part of the string starting with /dbname
		dbName_ = dbUriStr.substring(dbUriStr.lastIndexOf('/') + 1);
		collection_ = collection;
		index_ = index;
		machineName_ = machineName;
	}
	
	// Unfortunately, this also throws other exceptions that are not documetned...
    public DBCollection openMongoDb() throws UnknownHostException {

    	MongoClientURI dbUri = new MongoClientURI(dbUriStr_); //?? thros
	    mongoClient_ = new MongoClient(dbUri);
	    
	    DB db = mongoClient_.getDB( dbName_ );
	    DBCollection coll = db.getCollection(collection_);
	    coll.createIndex(new BasicDBObject(index_, 1));  // create index on "i", ascending
	    
	    return coll;
	 
    }
     
     public void closeMongoDb() {
         if(mongoClient_ != null) {
    	 	mongoClient_.close();
         }
     }

     public boolean WriteDebugDataToMongo(String message)
     {
    	 String complete = machineName_ + " " + new Date().toLocaleString() + " " + message;
    	 BasicDBObject doc = new BasicDBObject("DebugMessage", complete);
    	 return WriteToMongo(doc);
     }

     
     public boolean WriteToMongo(TransmitterRawData trd)
     {
    	 BasicDBObject bdbo = trd.toDbObj(machineName_ + " " + new Date(trd.CaptureDateTime).toLocaleString());
    	 return WriteToMongo(bdbo);
     }
     
     public boolean WriteToMongo(BasicDBObject bdbo)
     {
     	DBCollection coll;
     	try {
     		coll = openMongoDb();
         	coll.insert(bdbo);

 		} catch (UnknownHostException e) {
 			e.printStackTrace();
 			return false; 
 		} catch (MongoException e) {
 			e.printStackTrace();
 			return false; 
 		} catch (Exception e) {
 			e.printStackTrace();
 			closeMongoDb();
 			return false; 
 		}
     	finally {
 			closeMongoDb();
 		}
     	return true;
     }
     
     // records will be marked by their timestamp
     public List<TransmitterRawData> ReadFromMongo(int numberOfRecords) {
    	System.out.println( "Starting to read from mongodb"); 
    	 
    	List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
      	DBCollection coll;
      	TransmitterRawData lastTrd = null;
      	try {
      		coll = openMongoDb();
      		DBCursor cursor = coll.find();
            cursor.sort(new BasicDBObject("CaptureDateTime", -1));
            try {
                while(cursor.hasNext() && trd_list.size() < numberOfRecords) {
                    //System.out.println(cursor.next());
                    TransmitterRawData trd = new TransmitterRawData((BasicDBObject)cursor.next());
                    // Do our best to fix the relative time...
                    trd.RelativeTime = new Date().getTime() - trd.CaptureDateTime;
                    // since we are reading it from the db, it was uploaded...
                    trd.Uploaded = 1;
                    if(lastTrd == null) {
                    	trd_list.add(0,trd);
                    	lastTrd = trd;
                    	System.out.println( trd.toTableString());
                    } else if(!ReadData.almostEquals(lastTrd, trd)) {
                    	lastTrd = trd;
                    	trd_list.add(0,trd);
                    	System.out.println( trd.toTableString());
                    }
                    
                }
             } finally {
                cursor.close();
             }

  		} catch (UnknownHostException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  			return null; 
  		} catch (MongoException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  			return trd_list; 
  		} catch (Exception e) {
 			e.printStackTrace();
 			closeMongoDb();
 			return null; 
 		}finally {
  			closeMongoDb();
  		}
      	return trd_list;
    	 
     }


}
