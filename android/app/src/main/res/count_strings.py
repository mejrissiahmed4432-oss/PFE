import os
import re

layout_dir = r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\layout'

count = 0
for file in os.listdir(layout_dir):
    if file.endswith('.xml'):
        with open(os.path.join(layout_dir, file), 'r', encoding='utf-8') as f:
            content = f.read()
            matches = re.findall(r'android:(text|hint)="([^@][^"]*)"', content)
            count += len(matches)
print(f'Total hardcoded strings in layout: {count}')
