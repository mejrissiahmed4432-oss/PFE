
import requests

base_url = "http://localhost:8081/api/software"

software_data = [
    {"name": "Adobe Creative Cloud", "version": "2024.1", "vendor": "Adobe", "type": "Application", "status": "Active"},
    {"name": "Windows 11 Pro", "version": "23H2", "vendor": "Microsoft", "type": "OS", "status": "Active"},
    {"name": "IntelliJ IDEA Ultimate", "version": "2023.3", "vendor": "JetBrains", "type": "Tool", "status": "Active"},
    {"name": "Microsoft Office 2021", "version": "v16.0", "vendor": "Microsoft", "type": "Application", "status": "Active"},
    {"name": "Slack Enterprise Grid", "version": "v4.35", "vendor": "Slack", "type": "Service", "status": "Active"},
    {"name": "Docker Desktop", "version": "4.26.0", "vendor": "Docker", "type": "Tool", "status": "Active"},
    {"name": "Zoom Desktop Client", "version": "5.17.0", "vendor": "Zoom", "type": "Service", "status": "Active"},
    {"name": "AutoCAD 2024", "version": "R.47.0", "vendor": "Autodesk", "type": "Application", "status": "Active"},
    {"name": "GitHub Desktop", "version": "3.3.5", "vendor": "GitHub", "type": "Tool", "status": "Active"},
    {"name": "Visual Studio Code", "version": "1.85.1", "vendor": "Microsoft", "type": "Tool", "status": "Active"}
]

def update_software():
    try:
        # Get all software
        response = requests.get(base_url)
        if response.status_code == 200:
            existing = response.json()
            for sw in existing:
                for target in software_data:
                    if target["name"] == sw["name"]:
                        # Update it
                        sw.update(target)
                        put_resp = requests.put(f"{base_url}/{sw['id']}", json=sw)
                        if put_resp.status_code == 200:
                            print(f"Updated {sw['name']} with version {target['version']}")
                        else:
                            print(f"Failed to update {sw['name']}: {put_resp.status_code}")
                        break
        else:
            print(f"Failed to fetch software: {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    update_software()
