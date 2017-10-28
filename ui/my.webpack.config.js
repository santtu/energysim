var webpack = require('webpack');
var merge = require('webpack-merge');

var scalajs = require('./scalajs.webpack.config');
var myconfig = {
    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery",
            Popper: ['popper.js', 'default']
        })
    ],
    module: {
        loaders: [
            {
                test: /\.(scss)$/,
                use: [{
                    loader: 'style-loader', // inject CSS to page
                }, {
                    loader: 'css-loader', // translates CSS into CommonJS modules
                }, {
                    loader: 'postcss-loader', // Run post css actions
                    options: {
                        plugins: function () { // post css plugins, can be exported to postcss.config.js
                            return [
                                require('precss'),
                                require('autoprefixer')
                            ];
                        }
                    }
                }, {
                    loader: 'sass-loader' // compiles SASS to CSS
                }]
            }
        ]
    }
};

module.exports = merge(scalajs, myconfig)
