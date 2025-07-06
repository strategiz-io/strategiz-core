const path = require('path');

module.exports = {
  webpack: {
    configure: (webpackConfig, { env, paths }) => {
      // Find and configure ForkTsCheckerWebpackPlugin to prevent memory issues
      const forkTsCheckerPlugin = webpackConfig.plugins.find(
        plugin => plugin.constructor.name === 'ForkTsCheckerWebpackPlugin'
      );
      
      if (forkTsCheckerPlugin) {
        // Increase memory limit and add configurations to prevent crashes
        forkTsCheckerPlugin.options = {
          ...forkTsCheckerPlugin.options,
          memoryLimit: 8192, // 8GB memory limit
          typescript: {
            ...forkTsCheckerPlugin.options.typescript,
            memoryLimit: 8192,
            diagnosticOptions: {
              semantic: true,
              syntactic: true,
            },
          },
          eslint: {
            ...forkTsCheckerPlugin.options.eslint,
            memoryLimit: 4096, // 4GB for ESLint
          },
        };
      }

      // Optimize chunks for better memory usage
      if (env === 'development') {
        webpackConfig.optimization = {
          ...webpackConfig.optimization,
          splitChunks: {
            chunks: 'all',
            maxInitialRequests: 10,
            maxAsyncRequests: 10,
            cacheGroups: {
              vendor: {
                test: /[\\/]node_modules[\\/]/,
                name: 'vendors',
                chunks: 'all',
                enforce: true,
              },
            },
          },
        };
      }

      return webpackConfig;
    },
  },
  // Configure TypeScript for better memory usage
  typescript: {
    enableTypeChecking: true,
  },
  // Configure ESLint to use less memory
  eslint: {
    enable: true,
    mode: 'file',
  },
}; 