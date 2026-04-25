const { MongoClient } = require('mongodb');

async function repairData() {
  const uri = "mongodb://localhost:27017";
  const client = new MongoClient(uri);
  try {
    await client.connect();
    const db = client.db("stock-manager-db");
    
    // Find all parts that are already installed/assigned but still have a shelfId
    const result = await db.collection("equipment").updateMany(
      { 
        status: { $in: ["Installed", "Assigned", "Allocated"] },
        shelfId: { $ne: "" }
      },
      { $set: { shelfId: "" } }
    );
    
    console.log(`Repaired ${result.modifiedCount} equipment records (cleared shelfId for installed/allocated parts).`);
    
    // Also recalculate shelf counts
    console.log("Please restart your backend to trigger the shelf quantity sync.");
  } finally {
    await client.close();
  }
}

repairData();
