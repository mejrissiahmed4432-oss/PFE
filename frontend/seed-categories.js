const fetch = require('node-fetch'); // node > 18 has fetch builtin, let's just use http module if node < 18 or builtin fetch if node >= 18

const defaults = [
  { name: 'DEVICE', icon: 'monitor', types: ['Laptop', 'System Unit', 'Desktop', 'Smartphone', 'Tablet'] },
  { name: 'PERIPHERAL', icon: 'mouse', types: ['Monitor', 'Keyboard', 'Mouse', 'Printer', 'Scanner', 'Headset', 'Webcam', 'HDMI Cable', 'USB Cable', 'Charger', 'Adapter', 'Docking Station', 'USB Hub'] },
  { name: 'NETWORK', icon: 'wifi', types: ['Router', 'Switch', 'Access Point', 'Firewall'] },
  { name: 'STORAGE', icon: 'hard-drive', types: ['SSD', 'HDD', 'NVMe', 'USB Flash', 'Ext. HDD'] },
  { name: 'COMPONENT', icon: 'cpu', types: ['RAM', 'CPU', 'GPU', 'Motherboard', 'NIC'] }
];

async function seed() {
  for (const cat of defaults) {
    try {
      const res = await fetch('http://localhost:8000/api/equipment-categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(cat)
      });
      if (res.ok) {
        console.log(`Successfully added category: ${cat.name}`);
      } else {
        const text = await res.text();
        console.error(`Failed to add ${cat.name}: ${res.status} ${text}`);
      }
    } catch (e) {
      console.error(`Error adding ${cat.name}: ${e.message}`);
    }
  }
}

seed();
