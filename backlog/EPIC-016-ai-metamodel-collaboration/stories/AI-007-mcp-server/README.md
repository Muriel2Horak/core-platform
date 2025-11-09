# AI-007: MCP Server Implementation

**Status:** 🟡 **60% IN PROGRESS** (Prototype working, production features pending)  
**Effort:** 2 dny (prototyp hotov, zbývá DB access + Git integration)  
**Priority:** 🔥 HIGH  
**LOC:** ~1,200 (existing prototype)  
**Source:** EPIC-009 AI-001

---

## 📖 User Story

**As a developer**,  
I want Claude Desktop to access Core Platform codebase via MCP protocol,  
So that I can get AI-powered assistance with context about our backlog, code, and architecture.

---

## 🎯 Acceptance Criteria

- ✅ MCP server responds to Claude Desktop requests
- ✅ Tools implemented: `read_file`, `search_code`, `get_epic_summary`
- ✅ Server runs locally via Node.js
- ✅ Configuration in `claude_desktop_config.json` works
- ⏳ Database access via MCP tools (pending)
- ⏳ Git integration (branch status, diff viewer) (pending)

---

## 🏗️ Technical Implementation

### Architecture

```
┌─────────────────┐
│ Claude Desktop  │ (User interacts via chat)
└────────┬────────┘
         │ JSON-RPC (stdio)
         ▼
┌─────────────────┐
│   MCP Server    │ (Node.js TypeScript)
│  core-platform  │
└────────┬────────┘
         │
         ├─► read_file(path) → Returns file content
         ├─► search_code(query, glob?) → grep results
         └─► get_epic_summary(epicId) → Parses backlog/EPIC-XXX/README.md
```

### Server Implementation (Current - 60% Done)

**File:** `mcp-server/src/index.ts`

```typescript
#!/usr/bin/env node
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from "@modelcontextprotocol/sdk/types.js";
import { promises as fs } from "fs";
import path from "path";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);

// Workspace root (absolute path to core-platform)
const WORKSPACE_ROOT = process.env.WORKSPACE_ROOT || process.cwd();

// MCP Server Configuration
const server = new Server(
  {
    name: "core-platform-mcp",
    version: "0.1.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Tool Definitions
const TOOLS: Tool[] = [
  {
    name: "read_file",
    description: "Read contents of a file from Core Platform workspace",
    inputSchema: {
      type: "object",
      properties: {
        path: {
          type: "string",
          description: "Relative path from workspace root (e.g., 'backend/src/main/java/...')",
        },
      },
      required: ["path"],
    },
  },
  {
    name: "search_code",
    description: "Search for code patterns using grep (supports regex)",
    inputSchema: {
      type: "object",
      properties: {
        query: {
          type: "string",
          description: "Search pattern (supports regex)",
        },
        glob: {
          type: "string",
          description: "Optional file glob pattern (e.g., '*.java', 'backend/**/*.ts')",
        },
      },
      required: ["query"],
    },
  },
  {
    name: "get_epic_summary",
    description: "Get summary of EPIC (parses backlog/EPIC-XXX/README.md)",
    inputSchema: {
      type: "object",
      properties: {
        epicId: {
          type: "string",
          description: "EPIC ID (e.g., 'EPIC-001', 'EPIC-016')",
        },
      },
      required: ["epicId"],
    },
  },
];

// List Tools Handler
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: TOOLS,
}));

// Call Tool Handler
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "read_file": {
        const filePath = path.resolve(WORKSPACE_ROOT, args.path as string);
        
        // Security: Prevent directory traversal
        if (!filePath.startsWith(WORKSPACE_ROOT)) {
          throw new Error("Access denied: Path outside workspace");
        }

        const content = await fs.readFile(filePath, "utf-8");
        
        return {
          content: [
            {
              type: "text",
              text: `File: ${args.path}\n\n${content}`,
            },
          ],
        };
      }

      case "search_code": {
        const query = args.query as string;
        const glob = args.glob as string | undefined;

        let command = `grep -r -n "${query}" ${WORKSPACE_ROOT}`;
        if (glob) {
          command = `find ${WORKSPACE_ROOT} -name "${glob}" -exec grep -n "${query}" {} +`;
        }

        const { stdout } = await execAsync(command);

        return {
          content: [
            {
              type: "text",
              text: `Search results for "${query}":\n\n${stdout}`,
            },
          ],
        };
      }

      case "get_epic_summary": {
        const epicId = args.epicId as string;
        const epicPath = path.resolve(
          WORKSPACE_ROOT,
          `backlog/${epicId}/README.md`
        );

        const content = await fs.readFile(epicPath, "utf-8");

        // Parse first 50 lines for summary (title, status, stories)
        const lines = content.split("\n").slice(0, 50);
        const summary = lines.join("\n");

        return {
          content: [
            {
              type: "text",
              text: `EPIC Summary (${epicId}):\n\n${summary}`,
            },
          ],
        };
      }

      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    return {
      content: [
        {
          type: "text",
          text: `Error: ${errorMessage}`,
        },
      ],
      isError: true,
    };
  }
});

// Start Server
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Core Platform MCP Server running on stdio");
}

main().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});
```

### Claude Desktop Configuration

**File:** `~/Library/Application Support/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "core-platform": {
      "command": "node",
      "args": ["/Users/martinhorak/Projects/core-platform/mcp-server/build/index.js"],
      "env": {
        "WORKSPACE_ROOT": "/Users/martinhorak/Projects/core-platform"
      }
    }
  }
}
```

---

## ✅ What Works (60% Complete)

### Working Features
- ✅ **MCP Protocol**: Server responds to Claude via JSON-RPC stdio
- ✅ **read_file Tool**: Reads any file from workspace (security: prevents traversal)
- ✅ **search_code Tool**: grep-based code search with optional glob patterns
- ✅ **get_epic_summary Tool**: Parses EPIC README for quick summaries
- ✅ **Claude Integration**: Claude Desktop successfully calls tools
- ✅ **Error Handling**: Graceful errors returned to Claude

### Example Usage in Claude
```
User: "What's in the EPIC-016 README?"

Claude: [Calls get_epic_summary("EPIC-016")]
Response:
  EPIC Summary (EPIC-016):
  
  # EPIC-016: AI & ML Platform Integration
  Status: 🟡 30% IN PROGRESS
  ...
```

---

## ⏳ Pending Features (40% Remaining)

### 1. Database Access via MCP Tools (HIGH Priority)

**New Tool:** `query_metamodel`

```typescript
{
  name: "query_metamodel",
  description: "Query Core Platform metamodel (entities, attributes, relationships)",
  inputSchema: {
    type: "object",
    properties: {
      query: {
        type: "string",
        description: "Natural language query (e.g., 'Show all entities with PII')",
      },
    },
    required: ["query"],
  },
}
```

**Implementation Plan:**
- Connect to PostgreSQL via `pg` library
- Query `metamodel.entities`, `metamodel.attributes` tables
- Use AI to convert natural language → SQL query
- Return formatted results to Claude

**Effort:** 1 day

---

### 2. Git Integration (MEDIUM Priority)

**New Tools:**
- `git_status` - Show current branch, uncommitted changes
- `git_diff` - Show diff for specific file or all changes
- `git_branch_list` - List all branches with last commit info

**Implementation Plan:**
- Use `simple-git` library for Git operations
- Expose as MCP tools for Claude to call
- Enable Claude to help with Git workflows (e.g., "What files changed in this PR?")

**Effort:** 1 day

---

### 3. Project Structure Navigator (LOW Priority)

**New Tool:** `get_directory_tree`

```typescript
{
  name: "get_directory_tree",
  description: "Get directory structure tree (like 'tree' command)",
  inputSchema: {
    type: "object",
    properties: {
      path: {
        type: "string",
        description: "Relative path from workspace root",
      },
      depth: {
        type: "number",
        description: "Max depth (default: 2)",
      },
    },
    required: ["path"],
  },
}
```

**Effort:** 0.5 day

---

## 🧪 Testing

### Manual Testing Checklist
- ✅ Claude Desktop connects to MCP server (check Claude settings)
- ✅ `read_file` returns correct file content
- ✅ `search_code` finds expected matches
- ✅ `get_epic_summary` parses EPIC README correctly
- ✅ Security: Path traversal blocked (e.g., `read_file("../../etc/passwd")` fails)
- ⏳ `query_metamodel` connects to PostgreSQL (pending)
- ⏳ `git_diff` shows current changes (pending)

### Automated Testing (Planned)
```typescript
// mcp-server/tests/tools.test.ts
describe("MCP Tools", () => {
  test("read_file should return file content", async () => {
    const result = await callTool("read_file", { path: "README.md" });
    expect(result.content[0].text).toContain("# Core Platform");
  });

  test("search_code should find matches", async () => {
    const result = await callTool("search_code", { 
      query: "@RestController", 
      glob: "*.java" 
    });
    expect(result.content[0].text).toContain("GroupController.java");
  });

  test("get_epic_summary should parse EPIC README", async () => {
    const result = await callTool("get_epic_summary", { epicId: "EPIC-001" });
    expect(result.content[0].text).toContain("RBAC Framework");
  });
});
```

**Effort:** 0.5 day

---

## 📦 Dependencies

- **Node.js** 18+ (for MCP server)
- **@modelcontextprotocol/sdk** (MCP protocol implementation)
- **Claude Desktop** (client that calls MCP server)
- **TypeScript** (server codebase)
- **pg** (PostgreSQL client - for `query_metamodel` tool)
- **simple-git** (Git operations - for Git tools)

---

## 🎯 Business Value

### Developer Productivity
- **Context-Aware AI**: Claude knows about Core Platform codebase (EPICs, architecture)
- **Faster Navigation**: Ask "Where is GroupController?" instead of manual search
- **Code Understanding**: "Explain how RBAC works" with real project code
- **Onboarding**: New devs ask Claude instead of senior devs

### ROI
- **Time Savings**: 2-4 hours/week per developer (faster context switching)
- **Cost Avoidance**: $15k/year (reduced senior dev time for questions)
- **Quality**: AI provides consistent answers (vs. tribal knowledge)

---

## 🚀 Next Steps

1. **Complete Database Access** (1 day)
   - Implement `query_metamodel` tool
   - Connect to PostgreSQL via `pg` library
   - Test with Claude: "Show all entities with PII attributes"

2. **Add Git Integration** (1 day)
   - Implement `git_status`, `git_diff`, `git_branch_list` tools
   - Test with Claude: "What changed in this branch?"

3. **Production Deployment** (0.5 day)
   - Package MCP server as npm package
   - Document setup in README
   - CI/CD for auto-build on changes

4. **Write Tests** (0.5 day)
   - Unit tests for all tools
   - Integration tests with Claude Desktop

**Total Remaining Effort:** 3 days

---

## 📚 References

- [Model Context Protocol Specification](https://modelcontextprotocol.io/introduction)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)
- [Claude Desktop MCP Setup](https://modelcontextprotocol.io/quickstart/user)
- Project: `mcp-server/` directory in workspace

---

**Last Updated:** October 2024  
**Implementation:** 60% complete (prototype working, DB + Git pending)
