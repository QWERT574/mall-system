const sharp = require('sharp');
const path = require('path');
const fs = require('fs');

const inputName = process.argv[2] || 'usecase-order-management';
const svgPath = path.join(__dirname, '..', 'docs', `${inputName}.svg`);
const pngPath = path.join(__dirname, '..', 'docs', `${inputName}.png`);

const svg = fs.readFileSync(svgPath);

sharp(svg, { density: 300 })
  .resize({ width: 2400 })
  .png()
  .toFile(pngPath)
  .then(info => {
    console.log('PNG generated:', pngPath);
    console.log('Size:', info.width, 'x', info.height, '|', (info.size / 1024).toFixed(1), 'KB');
  })
  .catch(err => {
    console.error('Error:', err);
    process.exit(1);
  });
