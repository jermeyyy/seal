import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from '@/contexts/ThemeContext'
import { SearchProvider } from '@/contexts/SearchContext'
import Layout from '@components/Layout/Layout'
import SearchModal from '@components/Search/SearchModal'
import Home from '@pages/Home/Home'
import GettingStarted from '@pages/GettingStarted/GettingStarted'
import Demo from '@pages/Demo/Demo'

// Guide pages
import OkHttp from '@pages/Guides/OkHttp/OkHttp'
import Ktor from '@pages/Guides/Ktor/Ktor'
import CustomPolicies from '@pages/Guides/CustomPolicies/CustomPolicies'
import IOS from '@pages/Guides/iOS/iOS'
import Configuration from '@pages/Guides/Configuration/Configuration'

function App() {
  const basename = import.meta.env.PROD ? '/seal' : '/'
  
  return (
    <ThemeProvider>
      <SearchProvider>
        <BrowserRouter basename={basename}>
          <Layout>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/getting-started" element={<GettingStarted />} />

              {/* Guide subpages */}
              <Route path="/guides/okhttp" element={<OkHttp />} />
              <Route path="/guides/ktor" element={<Ktor />} />
              <Route path="/guides/custom-policies" element={<CustomPolicies />} />
              <Route path="/guides/ios" element={<IOS />} />
              <Route path="/guides/configuration" element={<Configuration />} />

              <Route path="/demo" element={<Demo />} />
              <Route path="*" element={<Home />} />
            </Routes>
          </Layout>
          <SearchModal />
        </BrowserRouter>
      </SearchProvider>
    </ThemeProvider>
  )
}

export default App
