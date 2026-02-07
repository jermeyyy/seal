export interface NavItem {
  label: string
  path?: string
  href?: string
  external?: boolean
  icon?: string
  children?: NavItem[]
}

export const navigationData: NavItem[] = [
  { label: 'Home', path: '/' },
  { label: 'Why CT?', path: '/why-ct' },
  { label: 'Getting Started', path: '/getting-started' },
  {
    label: 'Guides',
    children: [
      { label: 'OkHttp Integration', path: '/guides/okhttp' },
      { label: 'Ktor Integration', path: '/guides/ktor' },
      { label: 'Custom Policies', path: '/guides/custom-policies' },
      { label: 'iOS Integration', path: '/guides/ios' },
      { label: 'Configuration', path: '/guides/configuration' },
    ]
  },
  { label: 'Demo', path: '/demo' },
  {
    label: 'API Reference',
    href: '/seal/api/index.html',
    external: true
  },
  {
    label: 'GitHub',
    href: 'https://github.com/jermeyyy/seal',
    external: true,
    icon: 'github'
  }
]
