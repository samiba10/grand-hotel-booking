/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#1e3a5f', dark: '#152a45', light: '#e8eef6' },
        gold: { DEFAULT: '#c9a24a', light: '#f6edd8' },
      },
      fontFamily: {
        // System stacks with full Hebrew coverage - no external font (strict CSP).
        sans: ['system-ui', '-apple-system', '"Segoe UI"', 'Arial', '"Noto Sans Hebrew"', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
