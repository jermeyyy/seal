import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'

interface SearchResult {
  id: string
  title: string
  route: string
  content: string
}

interface SearchContextType {
  query: string
  results: SearchResult[]
  isOpen: boolean
  search: (q: string) => void
  openSearch: () => void
  closeSearch: () => void
}

const SearchContext = createContext<SearchContextType | undefined>(undefined)

export function SearchProvider({ children }: { children: ReactNode }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [isOpen, setIsOpen] = useState(false)

  const mockSearchData = [
    {
      id: 'home',
      title: 'Home',
      route: '/',
      content: 'Seal is a Certificate Transparency verification library for Kotlin Multiplatform'
    },
    {
      id: 'getting-started',
      title: 'Getting Started',
      route: '/getting-started',
      content: 'Learn how to install and configure Seal in your project'
    },
    {
      id: 'guides-okhttp',
      title: 'OkHttp Integration',
      route: '/guides/okhttp',
      content: 'Integrate Certificate Transparency verification with OkHttp using network interceptors'
    },
    {
      id: 'guides-ktor',
      title: 'Ktor Integration',
      route: '/guides/ktor',
      content: 'Use the Seal Ktor plugin for multiplatform Certificate Transparency'
    },
    {
      id: 'guides-custom-policies',
      title: 'Custom Policies',
      route: '/guides/custom-policies',
      content: 'Configure CT policies like Chrome CT Policy and Apple CT Policy'
    },
    {
      id: 'guides-ios',
      title: 'iOS Integration',
      route: '/guides/ios',
      content: 'iOS-specific Certificate Transparency with SecTrust and URLSession'
    },
    {
      id: 'guides-configuration',
      title: 'Configuration',
      route: '/guides/configuration',
      content: 'Complete configuration reference for host patterns, log lists, caching, and failure modes'
    },
    {
      id: 'demo',
      title: 'Demo',
      route: '/demo',
      content: 'See Seal in action with the demo application'
    }
  ]

  const search = (q: string) => {
    setQuery(q)
    if (q.length < 2) {
      setResults([])
      return
    }

    const filtered = mockSearchData.filter(item =>
      item.title.toLowerCase().includes(q.toLowerCase()) ||
      item.content.toLowerCase().includes(q.toLowerCase())
    )
    setResults(filtered)
  }

  const openSearch = () => setIsOpen(true)
  const closeSearch = () => {
    setIsOpen(false)
    setQuery('')
    setResults([])
  }

  // Keyboard shortcut (Cmd/Ctrl + K)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        openSearch()
      }
      if (e.key === 'Escape' && isOpen) {
        closeSearch()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen])

  return (
    <SearchContext.Provider value={{ query, results, isOpen, search, openSearch, closeSearch }}>
      {children}
    </SearchContext.Provider>
  )
}

export function useSearch() {
  const context = useContext(SearchContext)
  if (!context) throw new Error('useSearch must be used within SearchProvider')
  return context
}
