const { MongoClient, ObjectId } = require('mongodb');

async function checkData() {
    const uri = "mongodb+srv://admin:admin@cluster0.p7v8p.mongodb.net/stock_manager_db?retryWrites=true&w=majority";
    const client = new MongoClient(uri);

    try {
        await client.connect();
        const db = client.db("stock_manager_db");
        const equipment = db.collection("equipment");
        const shelves = db.collection("shelves");

        console.log("--- Equipment with CPU/Intel ---");
        const items = await equipment.find({
            type: { $regex: "CPU", $options: "i" },
            brand: { $regex: "Intel", $options: "i" }
        }).toArray();

        items.forEach(item => {
            console.log(`ID: ${item._id}, Name: ${item.equipmentName}, Status: ${item.status}, Qte: ${item.qte}, ShelfId: ${item.shelfId}`);
        });

        console.log("\n--- Relevant Shelves ---");
        const shelfIds = [...new Set(items.map(i => i.shelfId).filter(id => id))];
        const relevantShelves = await shelves.find({ _id: { $in: shelfIds.map(id => {
            try { return new ObjectId(id); } catch(e) { return id; }
        }) } }).toArray();

        relevantShelves.forEach(s => {
            console.log(`ID: ${s._id}, Nb: ${s.nb}, Type: ${s.equipmentType}, Current: ${s.currentQte}, Max: ${s.maxQte}`);
        });

    } catch (err) {
        console.error(err);
    } finally {
        await client.close();
    }
}

checkData();
