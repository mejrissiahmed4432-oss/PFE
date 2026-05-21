const fs = require('fs');
const html = fs.readFileSync('c:/Users/Asus/Desktop/Nouveau dossier/PFE/frontend/src/app/tickets/live-workbench/live-workbench.component.html', 'utf8');

const stack = [];
const lines = html.split('\n');
let errors = 0;

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  let strippedLine = line.replace(/<!--[\s\S]*?-->/g, '');
  
  const regex = /<div\b[^>]*>|<\/div>/gi;
  let match;
  while ((match = regex.exec(strippedLine)) !== null) {
    if (match[0].toLowerCase().startsWith('<div')) {
      let classMatch = match[0].match(/class="([^"]+)"/);
      let name = classMatch ? classMatch[1] : 'div';
      stack.push({ line: i + 1, name: name });
    } else {
      if (stack.length > 0) {
        stack.pop();
      } else {
        console.log(`EXTRA closing div at line ${i + 1}`);
        errors++;
      }
    }
  }
}
console.log(`Remaining open divs: ${stack.length}`);
if (stack.length > 0) {
  for (let s of stack) { console.log(`Unclosed: ${s.name} from line ${s.line}`); }
}
console.log(`Errors: ${errors}`);
