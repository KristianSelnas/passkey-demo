import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // The repo root also has a package-lock.json (for the `npm run dev` script that starts
  // both backend and frontend). Pin the root so Next.js doesn't pick the repo root instead.
  turbopack: {
    root: __dirname,
  },
};

export default nextConfig;
