import {writeFileSync} from 'node:fs';
writeFileSync('xparty-service/public/xparty/config.js', 'window.XPARTY_CONFIG = '+JSON.stringify({backendUrl:'https://pkxparty.onrender.com'})+';\n');
