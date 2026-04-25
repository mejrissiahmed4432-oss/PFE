const { MongoClient, ObjectId } = require('mongodb');

async function checkRam() {
  const uri = "mongodb://localhost:27017";
  const client = new MongoClient(uri);
  try {
    await client.connect();
    const db = client.db("stock-manager-db");
    const equipment = await db.collection("equipment").findOne({ _id: new ObjectId("69ea197eb76bf701ab37669a") });
    console.log("RAM Data:", JSON.stringify(equipment, null, 2));
  } finally {
    await client.close();
  }
}

checkRam();
