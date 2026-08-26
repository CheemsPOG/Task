/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig, type Plugin } from 'vite';

const root = path.dirname(fileURLToPath(import.meta.url));
const backendHost = process.env.BACKEND_HOST || '127.0.0.1';
const backendPort = Number(process.env.BACKEND_PORT || 8080);
const wsHost = process.env.WS_HOST || backendHost;
const wsPort = Number(process.env.WS_PORT || 8081);
const port = Number(process.env.PORT || 5173);

const MIME: Record<string, string> = {
	'.js': 'text/javascript',
	'.mjs': 'text/javascript',
	'.cjs': 'text/javascript',
	'.ts': 'text/javascript',
	'.css': 'text/css',
	'.json': 'application/json',
	'.svg': 'image/svg+xml',
	'.png': 'image/png',
	'.jpg': 'image/jpeg',
	'.jpeg': 'image/jpeg',
	'.gif': 'image/gif',
	'.woff': 'font/woff',
	'.woff2': 'font/woff2',
	'.ttf': 'font/ttf',
	'.map': 'application/json',
	'.html': 'text/html',
};

function serveStaticFolder(urlPrefix: string, folderName: string): Plugin {
	const dir = path.resolve(root, folderName);

	return {
		name: `serve-${folderName}`,
		configureServer(server) {
			server.middlewares.use((req, res, next) => {
				if (!req.url?.startsWith(urlPrefix)) {
					next();
					return;
				}

				const relative = decodeURIComponent(
					req.url.split('?')[0].slice(urlPrefix.length)
				);
				const file = path.resolve(dir, relative);
				if (!file.startsWith(dir) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
					next();
					return;
				}

				const ext = path.extname(file).toLowerCase();
				res.setHeader('Content-Type', MIME[ext] || 'application/octet-stream');
				fs.createReadStream(file).pipe(res);
			});
		},
		writeBundle() {
			const outDir = path.resolve(root, 'dist', folderName);
			fs.cpSync(dir, outDir, { recursive: true });
		},
	};
}

export default defineConfig({
	plugins: [
		serveStaticFolder('/charting_library/', 'charting_library'),
		serveStaticFolder('/datafeeds/', 'datafeeds'),
	],
	server: {
		host: '127.0.0.1',
		port,
		strictPort: true,
		proxy: {
			'/api': {
				target: `http://${backendHost}:${backendPort}`,
				changeOrigin: true,
				cookieDomainRewrite: '',
			},
			'/curpairs': {
				target: `http://${backendHost}:${backendPort}`,
				changeOrigin: true,
				cookieDomainRewrite: '',
			},
			'/ws': {
				target: `ws://${wsHost}:${wsPort}`,
				ws: true,
			},
		},
	},
});
