import fs from 'node:fs';
import http from 'node:http';
import net from 'node:net';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.dirname(fileURLToPath(import.meta.url));
const port = Number(process.env.PORT || 5173);
const backendHost = process.env.BACKEND_HOST || '127.0.0.1';
const backendPort = Number(process.env.BACKEND_PORT || 8080);

const MIME = {
	'.html': 'text/html; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.mjs': 'text/javascript; charset=utf-8',
	'.css': 'text/css; charset=utf-8',
	'.json': 'application/json; charset=utf-8',
	'.svg': 'image/svg+xml',
	'.png': 'image/png',
	'.jpg': 'image/jpeg',
	'.jpeg': 'image/jpeg',
	'.gif': 'image/gif',
	'.woff': 'font/woff',
	'.woff2': 'font/woff2',
	'.ttf': 'font/ttf',
	'.map': 'application/json',
	'.ico': 'image/x-icon',
};

function contentType(file) {
	return MIME[path.extname(file).toLowerCase()] || 'application/octet-stream';
}

function sendFile(res, file) {
	fs.stat(file, (error, stats) => {
		if (error || !stats.isFile()) {
			res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
			res.end('Not found');
			return;
		}

		res.writeHead(200, { 'Content-Type': contentType(file) });
		fs.createReadStream(file).pipe(res);
	});
}

function proxyHttp(req, res) {
	const request = http.request(
		{
			hostname: backendHost,
			port: backendPort,
			path: req.url,
			method: req.method,
			headers: {
				...req.headers,
				host: `${backendHost}:${backendPort}`,
			},
		},
		proxyRes => {
			res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
			proxyRes.pipe(res);
		}
	);

	request.on('error', error => {
		res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
		res.end(
			JSON.stringify({
				s: 'error',
				errmsg: `Backend unavailable at ${backendHost}:${backendPort}`,
				detail: error.message,
			})
		);
	});

	req.pipe(request);
}

function proxyWebSocket(req, socket, head) {
	const backend = net.connect(backendPort, backendHost, () => {
		const headerLines = Object.entries(req.headers)
			.map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join(', ') : value}`)
			.join('\r\n');
		backend.write(
			`${req.method} ${req.url} HTTP/1.1\r\n${headerLines}\r\n\r\n`
		);
		if (head?.length) {
			backend.write(head);
		}
		socket.pipe(backend);
		backend.pipe(socket);
	});

	backend.on('error', () => socket.destroy());
	socket.on('error', () => backend.destroy());
}

const server = http.createServer((req, res) => {
	const urlPath = decodeURIComponent((req.url || '/').split('?')[0]);

	if (urlPath.startsWith('/api/')) {
		proxyHttp(req, res);
		return;
	}

	const relative = urlPath === '/' ? 'index.html' : urlPath.replace(/^\/+/, '');
	const file = path.resolve(root, relative);
	if (!file.startsWith(root)) {
		res.writeHead(403).end('Forbidden');
		return;
	}

	sendFile(res, file);
});

server.on('upgrade', (req, socket, head) => {
	if ((req.url || '').startsWith('/ws/')) {
		proxyWebSocket(req, socket, head);
		return;
	}
	socket.destroy();
});

server.listen(port, '127.0.0.1', () => {
	console.log(`Frontend: http://127.0.0.1:${port}`);
	console.log(`Proxying /api and /ws to http://${backendHost}:${backendPort}`);
});
