import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        emergency: "#E11D48", // Neon Red
        police: "#2563EB",    // Deep Blue
        darkBg: "#020617",    // Midnight Navy
      },
    },
  },
  plugins: [],
};
export default config;