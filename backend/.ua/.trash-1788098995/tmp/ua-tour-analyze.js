#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");

function fail(msg) {
  process.stderr.write(String(msg) + "\n");
  process.exit(1);
}

function loadJson(filePath) {
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
  } catch (err) {
    fail(`Failed to read JSON ${filePath}: ${err.message}`);
  }
}

const CODE_ENTRY_NAMES = new Set([
  "index.ts", "index.js", "main.ts", "main.js", "app.ts", "app.js",
  "server.ts", "server.js", "mod.rs", "main.go", "main.py", "main.rs",
  "manage.py", "app.py", "wsgi.py", "asgi.py", "run.py", "__main__.py",
  "Application.java", "Main.java", "Program.cs", "config.ru", "index.php",
  "App.swift", "Application.kt", "main.cpp", "main.c",
  "ChartBackendApplication.java"
]);

function depthOf(filePath) {
  if (!filePath) return Infinity;
  const normalized = String(filePath).replace(/\\/g, "/").replace(/^\.\//, "");
  const parts = normalized.split("/").filter(Boolean);
  return parts.length - 1;
}

function isDocNode(node) {
  return node && (node.type === "document" || /\.md$/i.test(node.name || node.filePath || ""));
}

function main() {
  const inputPath = process.argv[2];
  const outputPath = process.argv[3];
  if (!inputPath || !outputPath) {
    fail("Usage: node ua-tour-analyze.js <input.json> <output.json>");
  }

  const input = loadJson(inputPath);
  const nodes = Array.isArray(input.nodes) ? input.nodes : [];
  const edges = Array.isArray(input.edges) ? input.edges : [];
  const layers = Array.isArray(input.layers) ? input.layers : [];

  const nodeById = new Map();
  for (const n of nodes) {
    if (n && n.id) nodeById.set(n.id, n);
  }

  const fanIn = new Map();
  const fanOut = new Map();
  for (const id of nodeById.keys()) {
    fanIn.set(id, 0);
    fanOut.set(id, 0);
  }

  const adjImportsCalls = new Map();
  const undirected = new Map();
  const forwardPairs = new Set();
  const containedBy = new Map();
  const allAdjImportsCalls = new Map();

  function addAdj(map, from, to) {
    if (!map.has(from)) map.set(from, new Set());
    map.get(from).add(to);
  }

  function resolveToKnown(id) {
    if (!id) return null;
    if (nodeById.has(id)) return id;
    if (containedBy.has(id) && nodeById.has(containedBy.get(id))) return containedBy.get(id);
    return null;
  }

  for (const e of edges) {
    if (!e || !e.source || !e.target) continue;
    const et = (e.type || "").toLowerCase();
    if (et === "contains") {
      containedBy.set(e.target, e.source);
    }
  }

  for (const e of edges) {
    if (!e || !e.source || !e.target) continue;
    if (nodeById.has(e.source)) fanOut.set(e.source, (fanOut.get(e.source) || 0) + 1);
    if (nodeById.has(e.target)) fanIn.set(e.target, (fanIn.get(e.target) || 0) + 1);

    const et = (e.type || "").toLowerCase();
    if (et === "imports" || et === "calls") {
      addAdj(allAdjImportsCalls, e.source, e.target);
      const fromFile = resolveToKnown(e.source);
      const toFile = resolveToKnown(e.target);
      if (fromFile && toFile && fromFile !== toFile) {
        addAdj(adjImportsCalls, fromFile, toFile);
        forwardPairs.add(`${fromFile}\t${toFile}`);
        addAdj(undirected, fromFile, toFile);
        addAdj(undirected, toFile, fromFile);
      }
    } else {
      const fromFile = resolveToKnown(e.source) || (nodeById.has(e.source) ? e.source : null);
      const toFile = resolveToKnown(e.target) || (nodeById.has(e.target) ? e.target : null);
      if (fromFile && toFile && fromFile !== toFile) {
        addAdj(undirected, fromFile, toFile);
        addAdj(undirected, toFile, fromFile);
      }
    }
  }

  const fanInRanking = [...nodeById.keys()]
    .map((id) => ({ id, fanIn: fanIn.get(id) || 0, name: nodeById.get(id).name || id }))
    .sort((a, b) => b.fanIn - a.fanIn || a.id.localeCompare(b.id))
    .slice(0, 20);

  const fanOutRanking = [...nodeById.keys()]
    .map((id) => ({ id, fanOut: fanOut.get(id) || 0, name: nodeById.get(id).name || id }))
    .sort((a, b) => b.fanOut - a.fanOut || a.id.localeCompare(b.id))
    .slice(0, 20);

  const fanOutValues = [...fanOut.values()].sort((a, b) => a - b);
  const fanInValues = [...fanIn.values()].sort((a, b) => a - b);
  const fanOutThreshold = fanOutValues.length
    ? fanOutValues[Math.max(0, Math.ceil(fanOutValues.length * 0.9) - 1)]
    : 0;
  const fanInCutoffIdx = Math.max(0, Math.floor(fanInValues.length * 0.25) - 1);
  const fanInLowCutoff = fanInValues.length ? fanInValues[fanInCutoffIdx] : 0;

  const entryPointCandidates = [];
  for (const node of nodes) {
    if (!node || !node.id) continue;
    let score = 0;
    const name = node.name || "";
    const filePath = node.filePath || "";
    const type = node.type || "";

    if (type === "document" || /\.md$/i.test(name) || /\.md$/i.test(filePath)) {
      const normalized = filePath.replace(/\\/g, "/");
      const depth = depthOf(normalized);
      if (/^README\.md$/i.test(name) && (depth === 0 || normalized === "README.md")) {
        score += 5;
      } else if (/README\.md$/i.test(normalized) || /^README\.md$/i.test(name)) {
        score += 5;
      } else if (/\.md$/i.test(name) && depth <= 1) {
        score += 2;
      }
    } else if (type === "file" || type === "config" || !type) {
      if (CODE_ENTRY_NAMES.has(name) || /Application\.java$/i.test(name) || /Application\.kt$/i.test(name)) {
        score += 3;
      }
      const d = depthOf(filePath);
      if (d <= 1) score += 1;
      const fo = fanOut.get(node.id) || 0;
      const fi = fanIn.get(node.id) || 0;
      if (fo >= fanOutThreshold && fo > 0) score += 1;
      if (fi <= fanInLowCutoff) score += 1;
    }

    if (score > 0) {
      entryPointCandidates.push({
        id: node.id,
        score,
        name: node.name || node.id,
        summary: node.summary || ""
      });
    }
  }
  entryPointCandidates.sort((a, b) => b.score - a.score || a.id.localeCompare(b.id));
  const topEntry = entryPointCandidates.slice(0, 5);

  let startNode = null;
  for (const c of entryPointCandidates) {
    const n = nodeById.get(c.id);
    if (n && !isDocNode(n) && (n.type === "file" || n.type === "config" || !n.type)) {
      startNode = c.id;
      break;
    }
  }
  if (!startNode) {
    const preferred = "file:src/main/java/com/task/chart/ChartBackendApplication.java";
    if (nodeById.has(preferred)) startNode = preferred;
    else {
      for (const n of nodes) {
        if (n && n.type === "file") {
          startNode = n.id;
          break;
        }
      }
    }
  }

  const order = [];
  const depthMap = {};
  const byDepth = {};
  if (startNode && nodeById.has(startNode)) {
    const visited = new Set([startNode]);
    const queue = [{ id: startNode, depth: 0 }];
    while (queue.length) {
      const { id, depth } = queue.shift();
      order.push(id);
      depthMap[id] = depth;
      if (!byDepth[depth]) byDepth[depth] = [];
      byDepth[depth].push(id);
      const nexts = adjImportsCalls.get(id) || new Set();
      for (const nxt of nexts) {
        if (!nodeById.has(nxt) || visited.has(nxt)) continue;
        visited.add(nxt);
        queue.push({ id: nxt, depth: depth + 1 });
      }
    }
  }

  const nonCodeFiles = {
    documentation: [],
    infrastructure: [],
    data: [],
    config: []
  };
  for (const n of nodes) {
    if (!n || !n.id) continue;
    const rec = { id: n.id, name: n.name || n.id, summary: n.summary || "", type: n.type };
    if (n.type === "document") nonCodeFiles.documentation.push(rec);
    else if (n.type === "service" || n.type === "pipeline" || n.type === "resource") {
      nonCodeFiles.infrastructure.push(rec);
    } else if (n.type === "table" || n.type === "schema" || n.type === "endpoint") {
      nonCodeFiles.data.push(rec);
    } else if (n.type === "config") {
      nonCodeFiles.config.push(rec);
    }
  }

  const bidirectionalPairs = [];
  const seenBi = new Set();
  for (const pair of forwardPairs) {
    const [a, b] = pair.split("\t");
    if (a === b) continue;
    if (forwardPairs.has(`${b}\t${a}`)) {
      const key = a < b ? `${a}\t${b}` : `${b}\t${a}`;
      if (!seenBi.has(key)) {
        seenBi.add(key);
        bidirectionalPairs.push([a, b]);
      }
    }
  }

  const parent = new Map();
  function find(x) {
    if (!parent.has(x)) parent.set(x, x);
    if (parent.get(x) !== x) parent.set(x, find(parent.get(x)));
    return parent.get(x);
  }
  function union(a, b) {
    const ra = find(a);
    const rb = find(b);
    if (ra !== rb) parent.set(ra, rb);
  }
  for (const [a, b] of bidirectionalPairs) union(a, b);

  const groups = new Map();
  for (const [a, b] of bidirectionalPairs) {
    const r = find(a);
    if (!groups.has(r)) groups.set(r, new Set());
    groups.get(r).add(a);
    groups.get(r).add(b);
  }

  for (const cluster of groups.values()) {
    let changed = true;
    while (changed) {
      changed = false;
      if (cluster.size >= 5) break;
      for (const [id, neighbors] of undirected.entries()) {
        if (cluster.has(id)) continue;
        let hits = 0;
        for (const m of cluster) {
          if (neighbors.has(m)) hits++;
        }
        if (hits >= 2) {
          cluster.add(id);
          changed = true;
          if (cluster.size >= 5) break;
        }
      }
    }
  }

  const clusters = [];
  for (const cluster of groups.values()) {
    const arr = [...cluster].filter((id) => nodeById.has(id));
    if (arr.length < 2) continue;
    let edgeCount = 0;
    const set = new Set(arr);
    for (const e of edges) {
      if (set.has(e.source) && set.has(e.target)) edgeCount++;
    }
    clusters.push({ nodes: arr.slice(0, 5), edgeCount });
  }
  clusters.sort((a, b) => b.edgeCount - a.edgeCount || b.nodes.length - a.nodes.length);
  const topClusters = clusters.slice(0, 10);

  const nodeSummaryIndex = {};
  for (const n of nodes) {
    if (!n || !n.id) continue;
    nodeSummaryIndex[n.id] = {
      name: n.name || n.id,
      type: n.type || "file",
      summary: n.summary || ""
    };
  }

  const result = {
    scriptCompleted: true,
    entryPointCandidates: topEntry,
    fanInRanking,
    fanOutRanking,
    bfsTraversal: {
      startNode,
      order,
      depthMap,
      byDepth
    },
    nonCodeFiles,
    clusters: topClusters,
    layers: {
      count: layers.length,
      list: layers.map((l) => ({
        id: l.id,
        name: l.name,
        description: l.description || ""
      }))
    },
    nodeSummaryIndex,
    totalNodes: nodes.length,
    totalEdges: edges.length
  };

  try {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    fs.writeFileSync(outputPath, JSON.stringify(result, null, 2), "utf8");
  } catch (err) {
    fail(`Failed to write output: ${err.message}`);
  }
}

main();
