import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Static pages only (the legal documents): published with Firebase Hosting, which the free plan includes.
  output: "export",
  trailingSlash: true,
};

export default nextConfig;
