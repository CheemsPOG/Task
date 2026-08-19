import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

const root = path.dirname(fileURLToPath(import.meta.url));

const MIME = {
	'.js': 'text/javascript',
	'.mjs': 'text/javascript',
	'.cjs': 'text/javascript',
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

function serveStaticFolder(urlPrefix, folderName) {
	const dir = path.resolve(root, folderName);

	return {
		name: `serve-${folderName}`,
		configureServer(server) {
			server.middlewares.use((req, res, next) => {
				if (!req.url?.startsWith(urlPrefix)) {
					next();
					return;
				}

				const relative = decodeURIComponent(req.url.split('?')[0].slice(urlPrefix.length));
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
		port: 5173,
		strictPort: true,
		proxy: {
			'/api': {
				target: 'http://127.0.0.1:8080',
				changeOrigin: true,
			},
			'/ws': {
				target: 'ws://127.0.0.1:8080',
				ws: true,
			},
		},
	},
});
