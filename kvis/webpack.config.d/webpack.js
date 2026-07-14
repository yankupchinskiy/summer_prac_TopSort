if (config.devServer) {
    config.devServer.client = {
        overlay: false
    };
    config.devServer.hot = true;
    config.devServer.open = false;
    config.devServer.port = 3000;
    config.devServer.historyApiFallback = true;
//    config.devServer.compress = false; // workaround for SSE
    config.devtool = 'eval-cheap-source-map';
} else {
    config.devtool = undefined;
}

// disable bundle size warning
config.performance = {
    assetFilter: function (assetFilename) {
      return !assetFilename.endsWith('.js');
    },
};

const webpack = require("webpack");
config.plugins.push(new webpack.ProvidePlugin({
    vis: "vis-network/standalone/esm/vis-network.js"
}));
