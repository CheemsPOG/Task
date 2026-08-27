/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

/**
 * Dev entry used by `npm start`. Loads vite.config.ts (proxies + library).
 * Production builds use `vite build` instead of this file.
 */

import { createServer } from 'vite';

const server = await createServer();
await server.listen();
server.printUrls();
