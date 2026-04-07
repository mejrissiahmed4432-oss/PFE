const { exec } = require('child_process');
const fs = require('fs');

exec('npx ng build', (error, stdout, stderr) => {
    fs.writeFileSync('build-check.txt', `STDOUT:\n${stdout}\n\nSTDERR:\n${stderr}\n\nERROR:\n${error ? error.message : 'none'}`);
});
