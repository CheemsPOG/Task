"use strict";

const fs = require("fs");
const path = require("path");

const nodes = JSON.parse(
  fs.readFileSync(
    path.join(__dirname, "arch-file-nodes.json"),
    "utf8"
  )
);

function layerOf(n) {
  const p = String(n.filePath || "").replace(/\\/g, "/");
  const name = n.name || "";
  if (n.type === "table") return "data";
  if (/\/db\/migration\//.test(p) || /\.sql$/i.test(name)) return "data";
  if (/\/entity\//.test(p) || /\/repository\//.test(p)) return "data";
  if (/\/controller\//.test(p)) return "api";
  if (/\/cache\//.test(p)) return "cache";
  if (name === "DemoMarket.java" || name === "ResolutionMapper.java") return "cache";
  if (/\/dto\//.test(p) || /\/constants\//.test(p)) return "types";
  if (name === "RefreshTokenSession.java" || name === "ChartPrincipal.java") {
    return "types";
  }
  if (/\/service\//.test(p)) return "service";
  if (/\/exception\//.test(p)) return "middleware";
  if (/\/security\//.test(p)) {
    if (name === "SecurityConfig.java") return "config";
    return "middleware";
  }
  if (name === "ChartBackendApplication.java" || /\/config\//.test(p)) {
    return "config";
  }
  if (
    name === "application.yml" ||
    name === "pom.xml" ||
    name === "messages.properties" ||
    name === "messages_ja.properties"
  ) {
    return "config";
  }
  if (
    name === "README.md" ||
    name === "package-info.java" ||
    name === "mvnw" ||
    name === "mvnw.cmd" ||
    name === ".gitattributes"
  ) {
    return "docs-tooling";
  }
  if (p.startsWith(".ua/") || p.includes("/.ua/") || p.startsWith(".ua")) {
    return "docs-tooling";
  }
  return "UNASSIGNED";
}

const by = {};
const un = [];
for (const n of nodes) {
  const L = layerOf(n);
  if (!by[L]) by[L] = [];
  by[L].push(n.id);
  if (L === "UNASSIGNED") un.push(n.id + " | " + n.filePath + " | " + n.type);
}

const meta = {
  api: {
    name: "API Layer",
    description:
      "REST controllers that expose the TradingView UDF datafeed, auth, currency-pair catalog, chart layouts, and template endpoints.",
  },
  service: {
    name: "Service Layer",
    description:
      "Business services and implementations for datafeed history, tenant-scoped layouts and templates, symbol catalog, mock bar seeding, auth, and i18n message resolution.",
  },
  data: {
    name: "Data Layer",
    description:
      "JPA entities, Spring Data repositories, Flyway SQL migrations, and warehouse table schemas for FX pairs, users, marks, layouts, templates, and OHLC fact tables.",
  },
  types: {
    name: "Types Layer",
    description:
      "Request and response DTOs, error and price-side value objects, and shared constants used as contracts across the chart backend APIs.",
  },
  config: {
    name: "Config Layer",
    description:
      "Spring Boot application entry, Maven POM, application.yml, AppProperties, OpenAPI, password and CORS beans, SecurityConfig, i18n bundles, and demo user seeding.",
  },
  middleware: {
    name: "Middleware Layer",
    description:
      "JWT filter, unauthorized entry point, customer context, cookie and refresh-token helpers, domain exceptions, and the global REST exception handler.",
  },
  cache: {
    name: "Cache / Market Data",
    description:
      "Redis bar cache, warehouse ChartBarRepository, demo tick engine, quote bus, ingest worker, cache namespaces, and resolution or DemoMarket helpers used to form live FX bars.",
  },
  "docs-tooling": {
    name: "Docs and Tooling",
    description:
      "Package and mentor README maps, Maven wrapper scripts, git attributes, and Understand-Anything ignore and language config for the chart backend.",
  },
};

const order = [
  "api",
  "service",
  "data",
  "types",
  "config",
  "middleware",
  "cache",
  "docs-tooling",
];

const layers = order.map((id) => {
  const ids = by[id] || [];
  if (!ids.length) {
    throw new Error("Empty layer: " + id);
  }
  return {
    id: "layer:" + id,
    name: meta[id].name,
    description: meta[id].description,
    nodeIds: ids.slice().sort(),
  };
});

const assigned = layers.reduce((s, L) => s + L.nodeIds.length, 0);
const allIds = new Set(nodes.map((n) => n.id));
const seen = new Set();
let dup = 0;
let extra = 0;
for (const L of layers) {
  for (const id of L.nodeIds) {
    if (seen.has(id)) dup += 1;
    seen.add(id);
    if (!allIds.has(id)) extra += 1;
  }
}
const missing = nodes.filter((n) => !seen.has(n.id)).map((n) => n.id);

if (un.length || assigned !== nodes.length || missing.length || dup || extra) {
  console.error(
    JSON.stringify({ un, assigned, total: nodes.length, missing, dup, extra }, null, 2)
  );
  process.exit(1);
}

const outPath = path.join(__dirname, "..", "intermediate", "layers.json");
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, JSON.stringify(layers, null, 2), "utf8");

for (const L of layers) {
  console.log(L.name + ": " + L.nodeIds.length);
}
console.log("total: " + assigned);
