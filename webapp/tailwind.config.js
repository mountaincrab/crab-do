/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT: '#1e1e2e',
          raised: '#2a2a3e',
          high: '#313145',
        },
      },
    },
  },
  plugins: [],
}
