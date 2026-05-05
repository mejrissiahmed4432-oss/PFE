import re

def resolve_conflict(filepath, keep='head'):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # match <<<<<<< HEAD, then anything until =======, then anything until >>>>>>> ...
    pattern = re.compile(r'<<<<<<< HEAD\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> [^\n\r]+\r?\n?', re.DOTALL)
    
    def replacer(match):
        if keep == 'head':
            # return head part, ensuring it ends with a newline if it was originally there
            return match.group(1) + '\n' if match.group(1) else ''
        else:
            return match.group(2) + '\n' if match.group(2) else ''
            
    new_content = pattern.sub(replacer, content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)

resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\reports\reports.component.ts', 'theirs')
resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\reports\reports.component.html', 'theirs')
resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\schedule\schedule.component.ts', 'theirs')
resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\schedule\task.model.ts', 'theirs')
resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\schedule\task.service.ts', 'theirs')
resolve_conflict(r'c:\pfe-project\Test\pfe_v2\PFE\frontend\src\app\tickets\live-workbench\live-workbench.component.html', 'head')
print("Conflicts resolved")
