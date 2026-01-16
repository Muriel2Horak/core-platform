import express from "express";
import { ApolloServer } from "apollo-server-express";
import { ApolloServerPluginLandingPageLocalDefault } from "apollo-server-core";
import { typeDefs } from "./schema";
import { resolvers } from "./resolvers";
import { createContext } from "./context";

const app = express();

const enablePlayground = process.env.GRAPHQL_PLAYGROUND === "true";

const server = new ApolloServer({
  typeDefs,
  resolvers,
  context: ({ req }) => createContext(req),
  introspection: true,
  plugins: enablePlayground
    ? [ApolloServerPluginLandingPageLocalDefault({ embed: true })]
    : [],
});

async function start() {
  await server.start();
  server.applyMiddleware({ app, path: "/graphql" });

  app.get("/health", (_req, res) => {
    res.json({ status: "ok" });
  });

  const port = Number(process.env.PORT || 4000);
  app.listen(port, () => {
    console.log(`BFF running at http://localhost:${port}/graphql`);
  });
}

start().catch((error) => {
  console.error("Failed to start BFF", error);
  process.exit(1);
});
