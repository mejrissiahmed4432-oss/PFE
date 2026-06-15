import os
import re

files = [
    r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values\strings.xml',
    r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values-fr\strings.xml'
]

def sanitize_android_string(match):
    tag_start = match.group(1)
    text = match.group(2)
    tag_end = match.group(3)
    
    if text:
        # First unescape previously escaped to avoid double escaping
        text = text.replace("\\'", "'").replace('\\"', '"')
        # Now escape
        text = text.replace("'", "\\'").replace('"', '\\"')
        # Replace unicode errors if any
        # text = text.encode('ascii', 'ignore').decode('ascii')
        
    return tag_start + text + tag_end

for filepath in files:
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Regex to find <string name="...">text</string>
        # We need to only process the content between > and </string>
        
        # Simple string content fixer
        new_content = re.sub(r'(<string[^>]*>)(.*?)(</string>)', sanitize_android_string, content, flags=re.DOTALL)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed {filepath}")
