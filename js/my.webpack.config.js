var webpack = require('webpack');

module.exports = require('./scalajs.webpack.config');

module.exports.plugins = (module.exports.plugins || []).concat([
    new webpack.ProvidePlugin({
        $: "jquery",
        jQuery: "jquery",
        "window.jQuery": "jquery",
        "Popper": "popper.js"
    })
]);
