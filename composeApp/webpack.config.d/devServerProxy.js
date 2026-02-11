// Proxy CT verification requests through dev server to avoid CORS errors.
// Maps /ct-proxy/{hostname}/{path} → https://{hostname}/{path}
config.devServer = config.devServer || {};
config.devServer.proxy = [
    {
        context: ['/ct-proxy'],
        target: 'https://localhost',
        changeOrigin: true,
        secure: true,
        router: function(req) {
            var match = req.url.match(/^\/ct-proxy\/([^/]+)/);
            if (match) {
                return 'https://' + match[1];
            }
            return 'https://localhost';
        },
        pathRewrite: function(path) {
            return path.replace(/^\/ct-proxy\/[^/]+/, '') || '/';
        },
        onProxyRes: function(proxyRes) {
            var location = proxyRes.headers['location'];
            if (!location) return;
            try {
                var parsed = new URL(location);
                proxyRes.headers['location'] = '/ct-proxy/' + parsed.host + parsed.pathname + parsed.search;
            } catch (e) {
                // Relative URL — leave as-is
            }
        }
    }
];
