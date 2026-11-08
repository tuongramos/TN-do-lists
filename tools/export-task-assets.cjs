const fs = require('node:fs');
const path = require('node:path');
const { Resvg } = require('@resvg/resvg-js');
const root = path.resolve(__dirname, '..');
const input = path.join(root, 'docs/figma/tasks');
const output = path.join(root, 'app/src/main/res/drawable-nodpi');
const sources = Object.fromEntries(['list', 'detail'].map(name =>
  [name, fs.readFileSync(path.join(input, name + '.svg'), 'utf8')]));
const fonts = fs.readdirSync(path.join(root, 'app/src/main/res/font'))
  .map(name => path.join(root, 'app/src/main/res/font', name));
function render(source, target, scale = 3) {
  fs.writeFileSync(target, new Resvg(source, {
    fitTo: { mode: 'zoom', value: scale },
    font: { fontFiles: fonts, loadSystemFonts: false, defaultFontFamily: 'Be Vietnam Pro' }
  }).render().asPng());
}
function icon(name, sourceName, box, prefixes) {
  const paths = [...sources[sourceName].matchAll(/<path\s[^>]*\/>/g)].map(match => match[0]);
  const body = prefixes.map(prefix => {
    const item = paths.find(item => item.includes(`d="${prefix}`));
    if (!item) throw new Error('Missing SVG path: ' + prefix);
    return item;
  }).join('');
  render(`<svg xmlns="http://www.w3.org/2000/svg" fill="none" width="${box[2]}" height="${box[3]}" viewBox="${box.join(' ')}">${body}</svg>`,
    path.join(output, name + '.png'));
}
icon('task_search', 'list', [35,155,18,18], ['M48.5 163.25','M50 170']);
icon('task_nav_home', 'list', [37.2,800.5,24,24], ['M40.2002 811']);
icon('task_nav_list', 'list', [113.6,800.5,24,24], ['M122.6 806.5','M117.6 806.5']);
icon('task_calendar', 'detail', [36,242,20,20], ['M40.1667 246.167','M38.5 250.333']);
icon('task_reminder', 'detail', [36,293,20,20], ['M41 299.667','M44.5835 310.5']);
icon('task_repeat', 'detail', [36,344,20,20], ['M50.1665 345.667','M38.5 353.167','M41.8333 362.333','M53.5 354.833']);
icon('task_edit', 'detail', [287,588,18,18], ['M296 603','M299.375 590.625']);
icon('task_trash', 'detail', [335,588,18,18], ['M337.25 592.5','M341 592.5','M349.25 592.5','M342.5 596.25']);
icon('task_plus', 'detail', [36,746,20,20], ['M45.9998 750.167']);
let list = sources.list.replace(/<text[\s\S]*?<\/text>/,'')
  .replace(/<g filter="url\(#filter0[^>]*>/,'<g>')
  .replace(/<g clip-path="url\(#clip0[^>]*>/,'<g>')
  .replace('width="396" height="879" viewBox="0 0 396 879"', 'width="390" height="844" viewBox="3 31 390 844"');
render(list, path.join(input, 'reference_list.png'), 1);
render(sources.detail, path.join(input, 'reference_detail.png'), 1);
console.log('Exported 9 task icons and 2 full-screen references.');
