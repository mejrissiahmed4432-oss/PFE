import os
import re

files = [
    r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values\strings.xml',
    r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values-fr\strings.xml'
]

def clean_android_string(match):
    tag_start = match.group(1)
    text = match.group(2)
    tag_end = match.group(3)
    
    if not text:
        return tag_start + tag_end

    # Remove outer quotes if they exist to avoid double quoting
    if text.startswith('"') and text.endswith('"') and len(text) > 1:
        text = text[1:-1]
        
    # Unescape everything first
    text = text.replace('\\"', '"').replace("\\'", "'").replace('\\n', '\n').replace('\\t', '\t')
    
    # Remove any rogue backslashes that might be causing "Invalid unicode escape sequence"
    text = text.replace('\\', '')
    
    # Re-escape quotes
    text = text.replace('"', '\\"')
    text = text.replace("'", "\\'")
    # Re-escape newlines
    text = text.replace('\n', '\\n').replace('\t', '\\t')

    return tag_start + text + tag_end

for filepath in files:
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # We need to process the content between > and </string>
        new_content = re.sub(r'(<string[^>]*>)(.*?)(</string>)', clean_android_string, content, flags=re.DOTALL)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Cleaned {filepath}")
