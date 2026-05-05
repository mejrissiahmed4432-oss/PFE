import os
import re

def resolve_conflict(filepath, keep='head'):
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    pattern = re.compile(r'<<<<<<< HEAD\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> [^\n\r]*\r?\n?', re.DOTALL)
    
    # Check if there are matches
    if not pattern.search(content):
        print(f"No conflicts found in {filepath}")
        return
        
    def replacer(match):
        if keep == 'head':
            return match.group(1) + '\n' if match.group(1) else ''
        else:
            return match.group(2) + '\n' if match.group(2) else ''
            
    new_content = pattern.sub(replacer, content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"Resolved {filepath} using {keep}")

resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\equipment\equipment-wizard\equipment-wizard.component.ts', 'head')
