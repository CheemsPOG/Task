#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");

function fail(msg) {
  process.stderr.write(String(msg) + "\n");
  process.exit(1);
}

function readJson(p) {
  try {
    return JSON.parse(fs.readFileSync(p, "utf8"));
  } catch (e) {
    fail(`Failed to read JSON ${p}: ${e.message}`);
  }
}

function commonPrefix(paths) {
  if (!paths.length) return "";
  const norm = paths.map((p) => String(p || "").replace(/\\/g, "/"));
  let prefix = norm[0];
  for (let i = 1; i < norm.length; i++) {
    const s = norm[i];
    let j = 0;
    const lim = Math.min(prefix.length, s.length);
    while (j < lim && prefix[j] === s[j]) j++;
    prefix = prefix.slice(0, j);
    if (!prefix) break;
  }
  const slash = prefix.lastIndexOf("/");
  if (slash >= 0) return prefix.slice(0, slash + 1);
  return "";
}

function topGroup(filePath, prefix) {
  const p = String(filePath || "").replace(/\\/g, "/");
  let rest = p;
  if (prefix && rest.startsWith(prefix)) rest = rest.slice(prefix.length);
  rest = rest.replace(/^\/+/, "");
  if (!rest) return "root";
  const parts = rest.split("/").filter(Boolean);
  if (parts.length <= 1) return "root";
  return parts[0];
}

function classifyPattern(dirName, filePath, fileName) {
  const p = String(filePath || "").replace(/\\/g, "/").toLowerCase();
  const n = String(fileName || "").toLowerCase();
  const d = String(dirName || "").toLowerCase();

  if (
    /\.test\./.test(n) ||
    /\.spec\./.test(n) ||
    /^test_/.test(n) ||
    /_test\.go$/.test(n) ||
    /test\.java$/.test(n) ||
    /_spec\.rb$/.test(n) ||
    /test\.php$/.test(n) ||
    /tests\.cs$/.test(n)
  ) {
    return "test";
  }
  if (/\.d\.ts$/.test(n)) return "types";
  if (n === "dockerfile" || /^docker-compose/.test(n)) return "infrastructure";
  if (/\.tf$/.test(n) || /\.tfvars$/.test(n)) return "infrastructure";
  if (n === "makefile") return "infrastructure";
  if (p.includes(".github/workflows/") || n === ".gitlab-ci.yml" || n === "jenkinsfile") {
    return "ci-cd";
  }
  if (/\.sql$/.test(n)) return "data";
  if (/\.graphql$/.test(n) || /\.gql$/.test(n) || /\.proto$/.test(n)) return "types";
  if (/\.md$/.test(n) || /\.rst$/.test(n)) return "documentation";
  if (
    n === "cargo.toml" ||
    n === "go.mod" ||
    n === "gemfile" ||
    n === "pom.xml" ||
    n === "build.gradle" ||
    n === "composer.json"
  ) {
    return "config";
  }
  if (n === "application.java" || n === "program.cs" || /application\.java$/.test(n)) {
    return "entry";
  }
  if (n === "package-info.java") return "documentation";
  if (n === "manage.py" || n === "config.ru") return "entry";
  if (n === "wsgi.py" || n === "asgi.py") return "config";
  if (p.includes("src/main/java") && (n === "index.ts" || n === "index.js")) return "entry";

  const dirRules = [
    [["routes", "api", "controllers", "controller", "endpoints", "handlers", "routers", "serializers", "blueprints"], "api"],
    [["services", "service", "core", "lib", "domain", "logic", "internal", "signals", "mailers", "jobs", "channels", "composables"], "service"],
    [["models", "db", "data", "persistence", "repository", "repositories", "entities", "entity", "migrations", "sql", "database", "schema"], "data"],
    [["components", "views", "pages", "ui", "layouts", "screens"], "ui"],
    [["middleware", "plugins", "interceptors", "guards", "security", "exception", "filter"], "middleware"],
    [["utils", "util", "helpers", "common", "shared", "tools", "pkg", "templatetags"], "utility"],
    [["config", "constants", "env", "settings", "management", "commands"], "config"],
    [["__tests__", "test", "tests", "spec", "specs"], "test"],
    [["types", "interfaces", "schemas", "contracts", "dtos", "dto", "request", "response"], "types"],
    [["hooks"], "hooks"],
    [["store", "state", "reducers", "actions", "slices"], "state"],
    [["assets", "static", "public"], "assets"],
    [["cmd", "bin"], "entry"],
    [["docs", "documentation", "wiki"], "documentation"],
    [["deploy", "deployment", "infra", "infrastructure", "k8s", "kubernetes", "helm", "charts", "terraform", "tf", "docker"], "infrastructure"],
    [[".github", ".gitlab", ".circleci"], "ci-cd"],
    [["cache"], "cache"],
  ];

  const segs = p.split("/").filter(Boolean);
  for (const [names, label] of dirRules) {
    if (names.includes(d)) return label;
    if (segs.some((s) => names.includes(s))) return label;
  }
  if (p.includes("src/main/java")) return "service";
  if (p.includes("src/test/java")) return "test";
  return "unknown";
}

function main() {
  const inPath = process.argv[2];
  const outPath = process.argv[3];
  if (!inPath || !outPath) fail("Usage: node ua-arch-analyze.js <input.json> <output.json>");

  const input = readJson(inPath);
  const fileNodes = Array.isArray(input.fileNodes) ? input.fileNodes : [];
  const importEdges = Array.isArray(input.importEdges) ? input.importEdges : [];
  const allEdges = Array.isArray(input.allEdges) ? input.allEdges : [];

  const paths = fileNodes.map((n) => n.filePath || "").filter(Boolean);
  const prefix = commonPrefix(paths);

  const directoryGroups = {};
  const nodeIdToGroup = {};
  for (const n of fileNodes) {
    const g = topGroup(n.filePath || n.name || "", prefix);
    if (!directoryGroups[g]) directoryGroups[g] = [];
    directoryGroups[g].push(n.id);
    nodeIdToGroup[n.id] = g;
  }

  const nodeTypeGroups = {};
  for (const n of fileNodes) {
    const t = n.type || "file";
    if (!nodeTypeGroups[t]) nodeTypeGroups[t] = [];
    nodeTypeGroups[t].push(n.id);
  }

  const fileFanOut = {};
  const fileFanIn = {};
  const adj = {};
  for (const n of fileNodes) {
    fileFanOut[n.id] = 0;
    fileFanIn[n.id] = 0;
    adj[n.id] = [];
  }
  for (const e of importEdges) {
    if (!e || !e.source || !e.target) continue;
    if (fileFanOut[e.source] !== undefined) fileFanOut[e.source] += 1;
    if (fileFanIn[e.target] !== undefined) fileFanIn[e.target] += 1;
    if (adj[e.source]) adj[e.source].push(e.target);
  }

  const interMap = {};
  const intraInternal = {};
  const groupTotal = {};
  for (const g of Object.keys(directoryGroups)) {
    intraInternal[g] = 0;
    groupTotal[g] = 0;
  }
  for (const e of importEdges) {
    const from = nodeIdToGroup[e.source];
    const to = nodeIdToGroup[e.target];
    if (!from || !to) continue;
    groupTotal[from] = (groupTotal[from] || 0) + 1;
    groupTotal[to] = (groupTotal[to] || 0) + 1;
    if (from === to) {
      intraInternal[from] = (intraInternal[from] || 0) + 1;
    } else {
      const key = from + "\t" + to;
      interMap[key] = (interMap[key] || 0) + 1;
    }
  }
  const interGroupImports = Object.keys(interMap)
    .sort()
    .map((k) => {
      const [from, to] = k.split("\t");
      return { from, to, count: interMap[k] };
    });

  const intraGroupDensity = {};
  for (const g of Object.keys(directoryGroups)) {
    const internalEdges = intraInternal[g] || 0;
    const totalEdges = groupTotal[g] || 0;
    intraGroupDensity[g] = {
      internalEdges,
      totalEdges,
      density: totalEdges ? Number((internalEdges / totalEdges).toFixed(4)) : 0,
    };
  }

  const crossMap = {};
  const idToType = {};
  for (const n of fileNodes) idToType[n.id] = n.type || "file";
  for (const e of allEdges) {
    if (!e || !e.source || !e.target) continue;
    const fromType = idToType[e.source];
    const toType = idToType[e.target];
    if (!fromType || !toType) continue;
    const key = [fromType, toType, e.type || "unknown"].join("\t");
    crossMap[key] = (crossMap[key] || 0) + 1;
  }
  const crossCategoryEdges = Object.keys(crossMap).map((k) => {
    const [fromType, toType, edgeType] = k.split("\t");
    return { fromType, toType, edgeType, count: crossMap[k] };
  });

  const patternMatches = {};
  const nodeById = {};
  for (const n of fileNodes) nodeById[n.id] = n;
  for (const [g, ids] of Object.entries(directoryGroups)) {
    const votes = {};
    for (const id of ids) {
      const n = nodeById[id];
      const label = classifyPattern(g, n.filePath, n.name);
      votes[label] = (votes[label] || 0) + 1;
    }
    let best = "unknown";
    let bestN = -1;
    for (const [lab, c] of Object.entries(votes)) {
      if (c > bestN) {
        best = lab;
        bestN = c;
      }
    }
    patternMatches[g] = best;
  }

  const infraFiles = [];
  let hasDockerfile = false;
  let hasCompose = false;
  let hasK8s = false;
  let hasTerraform = false;
  let hasCI = false;
  for (const n of fileNodes) {
    const p = String(n.filePath || n.name || "").replace(/\\/g, "/");
    const name = String(n.name || "").toLowerCase();
    if (name === "dockerfile" || /^dockerfile/i.test(name)) {
      hasDockerfile = true;
      infraFiles.push(p);
    }
    if (/^docker-compose/i.test(name)) {
      hasCompose = true;
      infraFiles.push(p);
    }
    if (/\.ya?ml$/.test(name) && /(k8s|kubernetes|helm|deployment|ingress)/i.test(p)) {
      hasK8s = true;
      infraFiles.push(p);
    }
    if (/\.tf$/.test(name) || /\.tfvars$/.test(name)) {
      hasTerraform = true;
      infraFiles.push(p);
    }
    if (p.includes(".github/workflows/") || name === ".gitlab-ci.yml" || name === "jenkinsfile") {
      hasCI = true;
      infraFiles.push(p);
    }
  }

  const schemaFiles = [];
  const migrationFiles = [];
  const dataModelFiles = [];
  const apiHandlerFiles = [];
  for (const n of fileNodes) {
    const p = String(n.filePath || "").replace(/\\/g, "/");
    const name = String(n.name || "");
    const tags = n.tags || [];
    if (/\.(graphql|gql|proto|prisma)$/i.test(name)) schemaFiles.push(p);
    if (/migration/i.test(p) || /^V\d+__/.test(name) || n.type === "table") {
      if (n.type === "file" && /\.sql$/i.test(name)) migrationFiles.push(p);
    }
    if (
      tags.includes("entity") ||
      tags.includes("data-model") ||
      /\/entity\//.test(p) ||
      n.type === "table"
    ) {
      dataModelFiles.push(n.id);
    }
    if (
      tags.includes("api-handler") ||
      tags.includes("controller") ||
      /\/controller\//.test(p)
    ) {
      apiHandlerFiles.push(n.id);
    }
  }

  const groupsWithReadme = new Set();
  for (const n of fileNodes) {
    const name = String(n.name || "").toLowerCase();
    if (name === "readme.md" || n.type === "document") {
      const g = nodeIdToGroup[n.id];
      if (g) groupsWithReadme.add(g);
    }
  }
  const allGroups = Object.keys(directoryGroups);
  const undocumentedGroups = allGroups.filter((g) => !groupsWithReadme.has(g));
  const docCoverage = {
    groupsWithDocs: groupsWithReadme.size,
    totalGroups: allGroups.length,
    coverageRatio: allGroups.length
      ? Number((groupsWithReadme.size / allGroups.length).toFixed(4))
      : 0,
    undocumentedGroups,
  };

  const pairCounts = {};
  for (const row of interGroupImports) {
    const a = row.from;
    const b = row.to;
    if (!pairCounts[a]) pairCounts[a] = {};
    if (!pairCounts[b]) pairCounts[b] = {};
    pairCounts[a][b] = (pairCounts[a][b] || 0) + row.count;
  }
  const dependencyDirection = [];
  const seenPairs = new Set();
  for (const row of interGroupImports) {
    const key = [row.from, row.to].sort().join("\t");
    if (seenPairs.has(key)) continue;
    seenPairs.add(key);
    const ab = (pairCounts[row.from] && pairCounts[row.from][row.to]) || 0;
    const ba = (pairCounts[row.to] && pairCounts[row.to][row.from]) || 0;
    if (ab > ba) dependencyDirection.push({ dependent: row.from, dependsOn: row.to });
    else if (ba > ab) dependencyDirection.push({ dependent: row.to, dependsOn: row.from });
  }

  const filesPerGroup = {};
  for (const [g, ids] of Object.entries(directoryGroups)) filesPerGroup[g] = ids.length;
  const nodeTypeCounts = {};
  for (const [t, ids] of Object.entries(nodeTypeGroups)) nodeTypeCounts[t] = ids.length;

  const result = {
    scriptCompleted: true,
    directoryGroups,
    nodeTypeGroups,
    crossCategoryEdges,
    interGroupImports,
    intraGroupDensity,
    patternMatches,
    deploymentTopology: {
      hasDockerfile,
      hasCompose,
      hasK8s,
      hasTerraform,
      hasCI,
      infraFiles,
    },
    dataPipeline: {
      schemaFiles,
      migrationFiles,
      dataModelFiles,
      apiHandlerFiles,
    },
    docCoverage,
    dependencyDirection,
    fileStats: {
      totalFileNodes: fileNodes.length,
      filesPerGroup,
      nodeTypeCounts,
    },
    fileFanIn,
    fileFanOut,
    commonPrefix: prefix,
  };

  try {
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, JSON.stringify(result, null, 2), "utf8");
  } catch (e) {
    fail(`Failed to write output: ${e.message}`);
  }
}

main();
