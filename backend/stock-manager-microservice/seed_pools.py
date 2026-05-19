
import requests
import random

base_url = "http://localhost:8081/api/software"

def update_pools():
    try:
        # Get all software
        response = requests.get(base_url)
        if response.status_code == 200:
            software_list = response.json()
            for sw in software_list:
                sw_id = sw["id"]
                # Get pools
                pool_resp = requests.get(f"{base_url}/{sw_id}/pools")
                if pool_resp.status_code == 200:
                    pools = pool_resp.json()
                    has_active_pool = False
                    for p in pools:
                        if (p.get("totalSeats") or 0) > 0:
                            has_active_pool = True
                    
                    if not has_active_pool:
                        # Create a fresh one with seats
                        total = random.randint(100, 1000)
                        available = random.randint(20, total)
                        pool_data = {
                            "licenseModel": "SUBSCRIPTION",
                            "activationMethod": "USER_LOGIN",
                            "totalSeats": total,
                            "availableSeats": available,
                            "renewalType": "AUTO_RENEW",
                            "softwareId": sw_id
                        }
                        requests.post(f"{base_url}/{sw_id}/pools", json=pool_data)
                        print(f"Refreshed pool for {sw['name']}: {available}/{total}")
                    else:
                        # Find the first active pool and print its seats
                        for p in pools:
                            if (p.get("totalSeats") or 0) > 0:
                                print(f"{sw['name']} already has pool with {p['availableSeats']}/{p['totalSeats']} seats.")
                                break
                else:
                    print(f"Failed to get pools for {sw['name']}")
        else:
            print(f"Failed to fetch software: {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    update_pools()
