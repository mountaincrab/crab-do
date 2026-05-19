import { createContext, useContext, useEffect, useState, ReactNode } from 'react'

export type Theme = 'deep-navy' | 'charcoal' | 'retro'

export const THEMES: { id: Theme; label: string; accent: string; bg: string }[] = [
  { id: 'deep-navy', label: 'Deep Navy', accent: '#4F7CFF', bg: '#0A1020' },
  { id: 'charcoal', label: 'Charcoal', accent: '#06B6D4', bg: '#0A0A0A' },
  { id: 'retro', label: 'Retro', accent: '#FF00CC', bg: '#1A0B1E' },
]

const STORAGE_KEY = 'crabdo:theme'

type Ctx = {
  theme: Theme
  setTheme: (t: Theme) => void
}

const ThemeContext = createContext<Ctx | null>(null)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    if (typeof window === 'undefined') return 'deep-navy'
    const stored = localStorage.getItem(STORAGE_KEY) as Theme | null
    if (stored && THEMES.some(t => t.id === stored)) return stored
    return 'deep-navy'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(STORAGE_KEY, theme)
  }, [theme])

  return (
    <ThemeContext.Provider value={{ theme, setTheme: setThemeState }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used inside ThemeProvider')
  return ctx
}
