/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)',
        surface: {
          DEFAULT: 'var(--surface)',
          raised: 'var(--surface-raised)',
          high: 'var(--surface-high)',
        },
        fg: {
          DEFAULT: 'var(--fg)',
          muted: 'var(--fg-muted)',
          faint: 'var(--fg-faint)',
          disabled: 'var(--fg-disabled)',
        },
        accent: {
          DEFAULT: 'var(--accent)',
          hover: 'var(--accent-hover)',
          soft: 'var(--accent-soft)',
          fg: 'var(--accent-fg)',
          text: 'var(--accent-text)',
        },
        success: {
          DEFAULT: 'var(--success)',
          text: 'var(--success-text)',
        },
        danger: {
          DEFAULT: 'var(--danger)',
          text: 'var(--danger-text)',
        },
        warning: 'var(--warning)',
        link: 'var(--link)',
      },
      borderColor: {
        DEFAULT: 'var(--border)',
        subtle: 'var(--border-subtle)',
        strong: 'var(--border-strong)',
      },
      fontFamily: {
        sans: ['Geist', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
        mono: ['Geist Mono', 'ui-monospace', 'SFMono-Regular', 'SF Mono', 'Menlo', 'Consolas', 'Liberation Mono', 'monospace'],
      },
      backgroundImage: {
        'grad-accent': 'var(--grad-accent)',
      },
      boxShadow: {
        card: 'var(--shadow-card)',
        raised: 'var(--shadow-raised)',
        dialog: 'var(--shadow-dialog)',
        fab: 'var(--shadow-fab)',
      },
      letterSpacing: {
        display: '-0.03em',
        tightish: '-0.015em',
      },
    },
  },
  plugins: [],
}
