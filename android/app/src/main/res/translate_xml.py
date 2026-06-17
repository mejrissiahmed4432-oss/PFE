import os
import re
import xml.etree.ElementTree as ET
import time
from xml.dom import minidom
from deep_translator import GoogleTranslator

layout_dir = r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\layout'
strings_en_path = r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values\strings.xml'
strings_fr_path = r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values-fr\strings.xml'

translator = GoogleTranslator(source='en', target='fr')

def sanitize_key(text):
    text = re.sub(r'[^a-zA-Z0-9]', '_', text.lower())
    text = re.sub(r'_+', '_', text)
    text = text.strip('_')
    # Limit key length to 40 chars
    text = text[:40]
    # Handle empty or short keys
    if len(text) < 2:
        return None
    return 'txt_' + text

def add_string_to_xml(file_path, key, value):
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
    except:
        root = ET.Element("resources")
        tree = ET.ElementTree(root)

    # Check if key already exists
    for child in root:
        if child.attrib.get('name') == key:
            return

    new_str = ET.SubElement(root, 'string', name=key)
    new_str.text = value

    # Save formatted
    xml_str = ET.tostring(root, encoding='utf-8')
    parsed = minidom.parseString(xml_str)
    pretty_xml = '\n'.join([line for line in parsed.toprettyxml(indent='    ').split('\n') if line.strip()])
    
    # Simple replace to prevent HTML entity encoding on normal chars where possible, but ET handles it mostly.
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(pretty_xml)

strings_dict = {}

count = 0
for file in os.listdir(layout_dir):
    if not file.endswith('.xml'):
        continue
    filepath = os.path.join(layout_dir, file)
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find android:text="..." or android:hint="..."
    # Note: we need to replace it in the original content safely
    
    def replacer(match):
        attr = match.group(1)
        original_text = match.group(2)
        
        # Don't translate very short strings or placeholders like "0" or "—" or hardcoded dates
        if len(original_text) < 2 or re.match(r'^[0-9\-\.\:]+$', original_text) or original_text in ["—", "---"]:
            return match.group(0)

        key = sanitize_key(original_text)
        if not key:
            return match.group(0)

        if key not in strings_dict:
            strings_dict[key] = original_text
            # Translate
            try:
                fr_text = translator.translate(original_text)
            except Exception as e:
                print(f"Error translating {original_text}: {e}")
                fr_text = original_text # fallback
            
            # Save to XMLs
            add_string_to_xml(strings_en_path, key, original_text)
            add_string_to_xml(strings_fr_path, key, fr_text)
            time.sleep(0.1) # rate limit

        return f'android:{attr}="@string/{key}"'

    new_content, num_subs = re.subn(r'android:(text|hint)="([^@\?][^"]*)"', replacer, content)
    
    if num_subs > 0:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        count += num_subs
        print(f"Updated {file} - {num_subs} replacements")

print(f"Extraction and translation complete. Total items processed: {count}")
