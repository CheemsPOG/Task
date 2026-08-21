/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

import { createServer } from 'vite';

const server = await createServer();
await server.listen();
server.printUrls();
