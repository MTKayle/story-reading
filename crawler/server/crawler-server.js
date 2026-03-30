import express from 'express';
import cors from 'cors';
import { spawn } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const webRoot = path.resolve(__dirname, '..');
const crawlersDir = path.join(webRoot, 'crawlers');

const PYTHON_BIN = process.env.PYTHON || process.env.PYTHON_PATH || 'python';
const PORT = Number(process.env.PORT || process.env.CRAWLER_PORT || 4000);

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));

const sanitizeLogs = (chunks = []) =>
	chunks
		.join('')
		.split(/\r?\n/)
		.map((line) => line.trim())
		.filter((line) => line.length > 0);

app.get('/health', (_req, res) => {
	res.json({ status: 'ok' });
});

app.post('/api/crawl', async (req, res) => {
	const { 
		url, 
		type = 'title', 
		skipChapters = false,
		skipApi = false,
		userId = process.env.STORY_SERVICE_USER_ID || '1',
		storyServiceUrl = process.env.STORY_SERVICE_URL || 'http://localhost:8085',
		useDb = true,  // Default to true in Docker environment
		dbHost = process.env.DB_HOST || 'localhost',
		dbPort = process.env.DB_PORT || '5432',
		dbName = process.env.DB_NAME || 'storydb',
		dbUser = process.env.DB_USER || 'postgres',
		dbPassword = process.env.DB_PASSWORD || 'postgres123'
	} = req.body ?? {};

	if (!url || typeof url !== 'string') {
		return res.status(400).json({ error: 'Thiếu tham số url' });
	}

	if (type !== 'title') {
		return res.status(501).json({ error: 'Hiện chỉ hỗ trợ crawl thông tin truyện (title).' });
	}

	const scriptPath = path.join(crawlersDir, 'crawl_title.py');
	const args = [scriptPath, '--url', url];
	if (skipChapters) {
		args.push('--skip-chapters');
	}
	if (skipApi) {
		args.push('--skip-api');
	}
	if (useDb) {
		args.push('--use-db');
		args.push('--db-host', dbHost);
		args.push('--db-port', dbPort);
		args.push('--db-name', dbName);
		args.push('--db-user', dbUser);
		args.push('--db-password', dbPassword);
	} else {
		if (userId) {
			args.push('--user-id', String(userId));
		}
		if (storyServiceUrl) {
			args.push('--story-service-url', storyServiceUrl);
		}
	}

	const stdoutChunks = [];
	const stderrChunks = [];

	try {
		let responded = false;
		const sendResponse = (status, payload) => {
			if (responded) return;
			responded = true;
			return res.status(status).json(payload);
		};

		const child = spawn(PYTHON_BIN, args, {
			cwd: webRoot,
			env: {
				...process.env,
				PYTHONIOENCODING: process.env.PYTHONIOENCODING || 'utf-8',
				PYTHONUTF8: process.env.PYTHONUTF8 || '1',
			},
			stdio: ['ignore', 'pipe', 'pipe'],
		});

		child.stdout.on('data', (chunk) => {
			stdoutChunks.push(chunk.toString());
		});

		child.stderr.on('data', (chunk) => {
			stderrChunks.push(chunk.toString());
		});

		child.on('error', (error) => {
			console.error('Crawler process error:', error);
			sendResponse(500, {
				error: error instanceof Error ? error.message : 'Không thể khởi chạy script Python.',
				logs: [
					...sanitizeLogs(stdoutChunks).map((line) => `[stdout] ${line}`),
					...sanitizeLogs(stderrChunks).map((line) => `[stderr] ${line}`),
				],
			});
		});

		child.on('close', (code) => {
			const logs = [
				...sanitizeLogs(stdoutChunks).map((line) => `[stdout] ${line}`),
				...sanitizeLogs(stderrChunks).map((line) => `[stderr] ${line}`),
			];

			if (code !== 0) {
				return sendResponse(500, {
					error: `Script kết thúc với mã ${code}.`,
					logs,
				});
			}

			return sendResponse(200, { code, logs });
		});
	} catch (error) {
		console.error('Unexpected server error:', error);
		return res.status(500).json({
			error: error instanceof Error ? error.message : 'Không thể khởi chạy crawler.',
			logs: [],
		});
	}
});

app.listen(PORT, () => {
	console.log(`Crawler server listening on http://localhost:${PORT}`);
});


