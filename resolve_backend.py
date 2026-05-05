import os
import subprocess

def resolve_file(filepath, keep='head'):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    out_lines = []
    in_conflict = False
    current_block = ''
    
    for line in lines:
        if line.startswith('<<<<<<< HEAD'):
            in_conflict = True
            current_block = 'head'
            continue
        elif line.startswith('======='):
            current_block = 'theirs'
            continue
        elif line.startswith('>>>>>>>'):
            in_conflict = False
            current_block = ''
            continue
            
        if not in_conflict:
            out_lines.append(line)
        else:
            if keep == 'head' and current_block == 'head':
                out_lines.append(line)
            elif keep == 'theirs' and current_block == 'theirs':
                out_lines.append(line)
                
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(out_lines)

# Find all conflicted files in ai-service
try:
    output = subprocess.check_output(['git', 'grep', '-l', '<<<<<<< HEAD', 'backend/ai-service']).decode('utf-8')
    files = output.strip().split('\n')
    for f in files:
        if f:
            abs_path = os.path.join(os.getcwd(), f)
            print(f"Resolving {f} using HEAD")
            resolve_file(abs_path, 'head')
except Exception as e:
    print(f"Error: {e}")
