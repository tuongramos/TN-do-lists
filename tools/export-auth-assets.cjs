const fs = require('node:fs');
const path = require('node:path');
const { Resvg } = require('@resvg/resvg-js');
const root = path.resolve(__dirname, '..');
const input = path.join(root, 'docs/figma/auth');
const output = path.join(root, 'app/src/main/res/drawable-nodpi');
const fonts = fs.readdirSync(path.join(root, 'app/src/main/res/font')).map(f => path.join(root, 'app/src/main/res/font', f));
const sources = Object.fromEntries(['login','home','profile'].map(n => [n, fs.readFileSync(path.join(input,n+'.svg'),'utf8')]));
const defs = source => source.match(/<defs>[\s\S]*<\/defs>/)[0];
const wrap = (body, box, source) => `<svg xmlns="http://www.w3.org/2000/svg" fill="none" width="${box[2]}" height="${box[3]}" viewBox="${box.join(' ')}">${body}${defs(source)}</svg>`;
function render(source, target, scale=3) {
  fs.writeFileSync(target, new Resvg(source, {fitTo:{mode:'zoom',value:scale},font:{fontFiles:fonts,loadSystemFonts:false,defaultFontFamily:'Be Vietnam Pro'}}).render().asPng());
}
function icon(name, sourceName, box, prefixes) {
  const source = sources[sourceName];
  const paths = [...source.matchAll(/<path\s[^>]*\/>/g)].map(m=>m[0]);
  const selected = prefixes.map(prefix => {
    const item = paths.find(p => p.includes(`d="${prefix}`));
    if(!item) throw new Error('Missing path '+prefix);
    return item;
  });
  render(wrap(selected.join(''),box,source),path.join(output,name+'.png'));
}
icon('auth_back','login',[31,101,24,24],['M46 119']);
icon('auth_sync','login',[31,498,18,18],['M46.75 507','M46.75 500.25']);
icon('auth_device','login',[31,526,18,18],['M36.25 527.5','M40 539.5']);
icon('home_bell','home',[340,104,24,24],['M345.5 112.333','M349.442 124.25','M354 105']);
icon('study','home',[47,326,20,20],['M48.667 331.833','M52 333.5']);
icon('life','home',[228,326,20,20],['M230.5 334.75']);
icon('task_chevron','home',[344,522,12,12],['M347.75 523.5']);
icon('nav_home','home',[37.2,800.5,24,24],['M40.2002 811']);
icon('nav_list','home',[113.6,800.5,24,24],['M122.6 806.5','M117.6 806.5']);
icon('nav_bell','home',[258.4,800.5,24,24],['M264.4 808.5','M268.7 821.5']);
icon('nav_settings','home',[334.8,800.5,24,24],['M338.8 806.5','M351.8 806.5','M345.8 812.5','M353.8 818.5']);
icon('profile_back','profile',[23,99,18,18],['M34.75 113.5']);
icon('profile_camera','profile',[218,209,20,20],['M223 215.875','M230.188 219.625']);
icon('profile_check','profile',[103,310,17,17],['M106.708 318.5']);
icon('profile_sync','profile',[39,633.5,20,20],['M56.5 643.5','M56.5002 636']);
icon('profile_lock','profile',[39,686,20,20],['M44.0002 695.167','M45.6665 695.167']);
icon('profile_chevron','profile',[343.22,690,12,12],['M347.22 692']);
icon('profile_logout','profile',[143.5,804,18,18],['M150.25 819.75','M155.5 809.25']);
const login = sources.login;
const logoStart = login.indexOf('<g filter="url(#filter1');
const logoEnd = login.indexOf('<text ',logoStart);
render(wrap(login.slice(logoStart,logoEnd),[1,202,148,148],login),path.join(output,'logo_login.png'));
const home = sources.home;
const plusStart = home.indexOf('<g filter="url(#filter1');
const plusEnd = home.indexOf('</g>',plusStart)+4;
render(wrap(home.slice(plusStart,plusEnd),[150,764,96,96],home),path.join(output,'home_add.png'));

// resvg does not support Figma's foreignObject conic gradient. Convert its
// exact 43% stop into a sector before making the reference PNG.
function normalizeRing(source) {
  const angle = (154.8-90)*Math.PI/180;
  const x = 317+36*Math.cos(angle), y = 221.5+36*Math.sin(angle);
  const ring = `<circle cx="317" cy="221.5" r="36" fill="white" fill-opacity="0.28"/><path d="M317 221.5 L317 185.5 A36 36 0 0 1 ${x} ${y} Z" fill="white"/>`;
  return source.replace(/<g clip-path="url\(#paint0_angular[\s\S]*?data-figma-gradient-fill="[^\n]*?\/>/,ring);
}
for(const name of ['login','home','profile']) {
  let source = normalizeRing(sources[name]);
  // Remove the section title and only the outer decorative phone mask.
  source = source.replace(/<text[\s\S]*?<\/text>/,'');
  source = source.replace(/<g filter="url\(#filter0[^>]*>/,'<g>');
  source = source.replace(/<g clip-path="url\(#clip0[^>]*>/,'<g>');
  source = source.replace(/width="396" height="879" viewBox="0 0 396 879"/,'width="390" height="844" viewBox="3 31 390 844"');
  if(name==='login') {
    // Explicit user-requested addition: Google at y=528 and Facebook at y=598.
    const start = source.indexOf('<g filter="url(#filter2');
    const end = source.indexOf('</g>',start)+4;
    const original = source.slice(start,end);
    const google = `<g transform="translate(0 -70)">${original}</g>`;
    const facebook = original.replace(/>G<\/tspan>/,'>f</tspan>').replace('>Ti&#x1ebf;p t&#x1ee5;c v&#x1edb;i Google</tspan>','>Ti&#x1ebf;p t&#x1ee5;c v&#x1edb;i Facebook</tspan>').replace('x="134.449"','x="124"').replace('x="96.5"','x="86.5"').replace('x="104.26"','x="94.26"');
    render(source.slice(0,start)+google+facebook+source.slice(end),path.join(input,'reference_login_two_providers.png'),1);
  } else {
    render(source,path.join(input,`reference_${name}.png`),1);
  }
}
console.log('Exported original icons, login logo, add button and three auth references.');
