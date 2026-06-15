import os

files = [
    r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values\strings.xml',
    r'c:\Users\Asus\Desktop\pfe final version\PFE\android\app\src\main\res\values-fr\strings.xml'
]

for filepath in files:
    if not os.path.exists(filepath): continue
    with open(filepath, 'r', encoding='utf-8') as f:
        for line_no, line in enumerate(f, 1):
            if '>' in line and '</' in line:
                content = line.split('>', 1)[1].rsplit('<', 1)[0]
                for i, c in enumerate(content):
                    if c == '\\':
                        next_char = content[i+1] if i+1 < len(content) else ''
                        if next_char not in ["'", '"', "\\", "n", "t", "u"]:
                            print(f'Invalid escape in {filepath}:{line_no}: \\{next_char} in {content}')
