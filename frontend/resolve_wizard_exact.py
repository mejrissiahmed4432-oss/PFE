import os

def resolve_file(filepath, keep='theirs'):
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

resolve_file(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\equipment\equipment-wizard\equipment-wizard.component.ts', 'theirs')
