import urllib.request
import json
import urllib.error

url = 'http://localhost:8085/api/ai/predict-maintenance'
data = {
    "equipmentId": "test",
    "serialNumber": "123",
    "equipmentName": "name",
    "department": "dept",
    "historicalMetrics": []
}

req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})

try:
    response = urllib.request.urlopen(req)
    print("Success:", response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("Error:", e.code)
    print("Body:", e.read().decode('utf-8'))
