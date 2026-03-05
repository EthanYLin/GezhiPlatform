import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/:path*",
      },
    ];
  },
  images: {
    formats: ["image/avif", "image/webp"],
    unoptimized: false,
  },
};

export default nextConfig;
