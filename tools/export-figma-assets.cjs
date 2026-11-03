// Run with Node.js after installing @resvg/resvg-js@2.6.2 in tools/.
// Only the original logo and illustration layers are exported; UI stays in Compose.
const fs = require('node:fs');
const path = require('node:path');
const { Resvg } = require('@resvg/resvg-js');
const root = path.resolve(__dirname, '..');
const originals = path.join(root, 'docs', 'figma');
const output = path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi');
const fontDir = path.join(root, 'app', 'src', 'main', 'res', 'font');
const fonts = fs.readdirSync(fontDir).filter(f => f.endsWith('.ttf')).map(f => path.join(fontDir, f));
const defs = svg => svg.slice(svg.indexOf('<defs>'), svg.indexOf('</defs>') + 7);
const wrap = (body, box, definitions, background) =>
    `<svg xmlns="http://www.w3.org/2000/svg" fill="none" width="${box[2]}" height="${box[3]}" viewBox="${box.join(' ')}">${background || ''}${body}${definitions}</svg>`;
function render(svg, file, scale = 3) {
    const result = new Resvg(svg, {
        fitTo: { mode: 'zoom', value: scale },
        font: { fontFiles: fonts, loadSystemFonts: false, defaultFontFamily: 'Be Vietnam Pro' }
    }).render();
    fs.writeFileSync(file, result.asPng());
}
fs.mkdirSync(output, { recursive: true });
for (let step = 1; step <= 3; step++) {
    const svg = fs.readFileSync(path.join(originals, `onboarding_${step}.svg`), 'utf8');
    const start = svg.indexOf('<rect x="88"');
    // The reminder's miniature text belongs to its illustration group.
    const title = svg.indexOf('<text fill="#1C1C1E" style=', start);
    const titleStart = svg.indexOf('font-size="28"', title);
    const actualEnd = svg.lastIndexOf('<text ', titleStart);
    const originY = step === 2 ? 201.99 : 202;
    const illustration = svg.slice(start, actualEnd);
    render(wrap(illustration, [58, originY, 280, 300], defs(svg)),
        path.join(output, `illustration_${step}.png`));
    // Full-screen references, with the Figma section label and outer shadow removed.
    const frameStart = svg.indexOf('<rect x="3"');
    const frameEnd = svg.indexOf('</g>\n</g>\n<defs>');
    const inner = svg.slice(frameStart, frameEnd);
    render(wrap(inner, [3, step === 2 ? 30.99 : 31, 390, 844], defs(svg)),
        path.join(originals, `reference_${step}.png`), 1);
    if (step === 1) {
        const logoStart = svg.indexOf('<rect x="31"');
        const logoEnd = svg.indexOf('<text ', logoStart);
        render(wrap(svg.slice(logoStart, logoEnd), [31, 97, 32, 32], defs(svg)),
            path.join(output, 'logo_header.png'));
    }
}
const splash = fs.readFileSync(path.join(originals, 'start.svg'), 'utf8');
render(wrap(splash.slice(splash.indexOf('<g filter='), splash.indexOf('<text ')),
    [102, 258, 196, 196], defs(splash)), path.join(output, 'logo_splash.png'));
render(splash, path.join(originals, 'reference_start.png'), 1);
console.log('Exported 5 original assets at 3x and 4 full-screen references.');
