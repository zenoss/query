/**
 * dependency.js
 * Dependency injection utils
 */
(function(){
    "use strict";

    /**
     * Used to track dependency loading, including the load state
     * (loaded / loading) as well as the callback that will be called
     * when a dependency load has been completed.
     *
     * @access private
     */
    var __dependencies = {};

    /**
     * Main entry point for web pages. This method is used to first
     * bootstrap the library and then call the callback to create
     * charts. Because of the updated dependency loading capability,
     * this method is not strictly needed any more, but will be left
     * around for posterity.
     *
     * @param {callback}
     *            callback method called after all the pre-requisite
     *            JavaScript libraries are loaded.
     */
    function load(callback) {
        __bootstrap(callback);
    }

    /**
     * Given a dependency object, checks if the dependencies are already loaded
     * and if so, calls the callback, else loads the dependencies and then calls
     * the callback.
     *
     * @access private
     * @param {object}
     *            required the dependency object that contains a "defined" key
     *            and a "source" key. The "defined" key is a name (string) that
     *            is used to identify the dependency and the "source" key is an
     *            array of JavaScript and CSS URIs that must be loaded to meet
     *            the dependency.
     * @param {function}
     *            callback called after the dependencies are loaded
     */
    function __loadDependencies(required, callback) {
        var base, o, c, js, css;

        if (required === undefined) {
            callback();
            return;
        }

        // Check if it is already loaded, using the value in the 'defined' field
        if (__dependencies[required.defined] !== undefined &&
                __dependencies[required.defined].state !== undefined) {
            o = __dependencies[required.defined].state;
        }
        if (o !== undefined) {
            if (o === 'loaded') {
                if (debug.debug) {
                    debug.__log('Dependencies for "' + required.defined + '" already loaded, continuing.');
                }
                // Already loaded, so just invoke the callback
                callback();
            } else {
                // It is in the process of being loaded, so add our callback to
                // the
                // list of callbacks to call when it is loaded.
                if (debug.debug) {
                    debug.__log('Dependencies for "' + required.defined + '" in process of being loaded, queuing until loaded.');
                }

                c = __dependencies[required.defined].callbacks;
                c.push(callback);
            }
            return;
        } else {
            // OK, not yet loaded or being loaded, so it is ours.
            if (debug.debug) {
               debug.__log('Dependencies for "' + required.defined + '" not loaded nor in process of loading, initiate loading.');
            }

            __dependencies[required.defined] = {};
            base = __dependencies[required.defined];
            base.state = 'loading';
            base.callbacks = [];
            base.callbacks.push(callback);
        }

        // Load the JS and CSS files. Divide the list of files into two lists:
        // JS
        // and CSS as we can load one async, and the other loads sync (CSS).
        js = [];
        css = [];
        required.source.forEach(function(v) {
            if (v.endsWith('.js')) {
                js.push(v);
            } else if (v.endsWith('.css')) {
                css.push(v);
            } else {
                debug.__warn('Unknown required file type, "' +
                    '" when loading dependencies for "' + 'unknown' +
                    '". Ignored.');
            }
        });

        base = __dependencies[required.defined];
        __load(js, css, function() {
            base.state = 'loaded';
            base.callbacks.forEach(function(c) {
                c();
            });
            base.callbacks.length = 0;
        });
    }

    /**
     * Loads the CSS specified by the URL.
     *
     * @access private
     * @param {url}
     *            url the url, in string format, of the CSS file to load.
     */
    function __loadCSS(url) {
        var css = document.createElement('link');
        css.rel = 'stylesheet';
        css.type = 'text/css';

        if (!url.startsWith("http")) {
            css.href = visualization.url + visualization.urlPath + url;
        } else {
            css.href = url;
        }
        document.getElementsByTagName('head')[0].appendChild(css);
    }

    /**
     * We would like to use jQuery for dynamic loading of JavaScript files, but
     * it may be that jQuery is not yet loaded, so we first have to dynamically
     * load jQuery. To accomplish this we need a 'bootstrap' loader. This method
     * will load the JavaScript file specified by the URL by creating a new HTML
     * script element on the page and then call the callback once the script has
     * been loaded.
     *
     * @access private
     * @param {url}
     *            url URL, in string form, of the JavaScript file to load
     * @param {function}
     *            callback the function to call once the JavaScript is loaded
     */
    function __bootstrapScriptLoader(url, callback) {
        var script, deferred, _callback;

        function ZenDeferred() {
            var failCallback;

            this.fail = function(_) {
                if (!arguments.length) {
                    return failCallback;
                }
                failCallback = _;
                return failCallback;
            };
        }

        script = document.createElement("script");
        deferred = new ZenDeferred();
        _callback = callback;
        script.type = "text/javascript";
        script.async = true;

        if (script.readyState) { // IE
            script.onreadystatechange = function() {
                if (script.readyState === "loaded") {
                    var fail = deferred.fail();
                    if (fail !== undefined && fail !== null) {
                        fail();
                    }
                }
                if (script.readyState === "complete") {
                    script.onreadystatechange = null;
                    _callback();
                }
            };
        } else { // Others
            script.onload = function() {
                _callback();
            };
            script.onerror = function(e) {
                var fail = deferred.fail();
                if (fail !== undefined && fail !== null) {
                    fail(undefined, undefined, e.type);
                }
            };
        }

        script.src = url;
        document.getElementsByTagName("head")[0].appendChild(script);
        return deferred;
    }

    /**
     * Loads the array of JavaScript URLs followed by the array of CSS URLs and
     * calls the appropriate callback if the operations succeeded or failed.
     *
     * @access private
     * @param {uri[]}
     *            js an array of JavaScript files to load
     * @param {uri[]}
     *            css an array of CSS files to load
     * @param {function}
     *            [success] callback to call one everything is loaded
     * @param {function}
     *            [fail] callback to call if there is a failure
     */
    function __load(js, css, success, fail) {
        if (debug.debug) {
            debug.__log('Request to load "' + js + '" and "' + css + '".');
        }
        if (js.length === 0) {
            // All JavaScript files are loaded, now the loading of CSS can begin
            css.forEach(function(uri) {
                __loadCSS(uri);
                if (debug.debug) {
                    debug.__log('Loaded dependency "' + uri + '".');
                }
            });
            if (typeof success === 'function') {
                success();
            }
            return;
        }

        // Shift the next value off of the JS array and load it.
        var uri = js.shift(),
            _js = js,
            _css = css,
            loader;

        if (!uri.startsWith("http")) {
            uri = visualization.url + visualization.urlPath + uri;
        }

        loader = __bootstrapScriptLoader;

        if (window.jQuery !== undefined) {
            loader = $.getScript;
        }

        loader(uri, function() {
            if (debug.debug) {
                debug.__log('Loaded dependency "' + uri + '".');
            }
            __load(_js, _css, success, fail);

        }).fail(function(_1, _2, exception) {
            debug.__error('Unable to load dependency "' + uri + '", with error "' +
                exception + '". Loading halted, will continue, but additional errors likely.');
            __load(_js, _css, success, fail);
        });
    }

    /**
     * Loads jQuery and D3 as a dependencies and then calls the appripriate
     * callback.
     *
     * @access private
     * @param {function}
     *            [success] called if the core dependencies are loaded
     * @param {function}
     *            [fail] called if the core dependencies are not loaded
     */
    function __bootstrap(success, fail) {
        // dependencies that must be met for visualization.js
        // to work properly
        var depChecks = [
            {
                source: "jquery.min.js",
                check: function(){
                    return !!window.jQuery;
                }
            },{
                source: "jquery-ui.min.js",
                check: function(){
                    return !!window.jQuery && !!window.jQuery.tooltip;
                }
            },{
                source: "d3.v3.min.js",
                check: function(){
                    // TODO - check window.d3.version
                    return !!window.d3;
                }
            },{
                source: "moment.min.js",
                check: function(){
                    return !!window.moment;
                }
            },{
                source: "moment-timezone.js",
                check: function(){
                    return !!window.moment.tz;
                }
            },{
                source: "sprintf.min.js",
                check: function(){
                    return !!window.sprintf;
                }
            }
        ];
        var sources = [];

        // check if a dependency should be loaded or not
        depChecks.forEach(function(depCheck){
            if(!depCheck.check()){
                sources.push(depCheck.source);
            }
        });

        __loadDependencies({
            'defined' : 'd3',
            'source' : sources
        }, success, fail);
    }

    dependency = {

        /**
         * Used to track dependency loading, including the load state
         * (loaded / loading) as well as the callback that will be called
         * when a dependency load has been completed.
         *
         * @access private
         */
        __dependencies: __dependencies,

        /**
         * Main entry point for web pages. This method is used to first
         * bootstrap the library and then call the callback to create
         * charts. Because of the updated dependency loading capability,
         * this method is not strictly needed any more, but will be left
         * around for posterity.
         *
         * @param {callback}
         *            callback method called after all the pre-requisite
         *            JavaScript libraries are loaded.
         */
        load: load,

        /**
         * Given a dependency object, checks if the dependencies are already loaded
         * and if so, calls the callback, else loads the dependencies and then calls
         * the callback.
         *
         * @access private
         * @param {object}
         *            required the dependency object that contains a "defined" key
         *            and a "source" key. The "defined" key is a name (string) that
         *            is used to identify the dependency and the "source" key is an
         *            array of JavaScript and CSS URIs that must be loaded to meet
         *            the dependency.
         * @param {function}
         *            callback called after the dependencies are loaded
         */
        __loadDependencies: __loadDependencies,

        /**
         * Loads the CSS specified by the URL.
         *
         * @access private
         * @param {url}
         *            url the url, in string format, of the CSS file to load.
         */
        __loadCSS: __loadCSS,

        /**
         * We would like to use jQuery for dynamic loading of JavaScript files, but
         * it may be that jQuery is not yet loaded, so we first have to dynamically
         * load jQuery. To accomplish this we need a 'bootstrap' loader. This method
         * will load the JavaScript file specified by the URL by creating a new HTML
         * script element on the page and then call the callback once the script has
         * been loaded.
         *
         * @access private
         * @param {url}
         *            url URL, in string form, of the JavaScript file to load
         * @param {function}
         *            callback the function to call once the JavaScript is loaded
         */
        __bootstrapScriptLoader: __bootstrapScriptLoader,

        /**
         * Loads the array of JavaScript URLs followed by the array of CSS URLs and
         * calls the appropriate callback if the operations succeeded or failed.
         *
         * @access private
         * @param {uri[]}
         *            js an array of JavaScript files to load
         * @param {uri[]}
         *            css an array of CSS files to load
         * @param {function}
         *            [success] callback to call one everything is loaded
         * @param {function}
         *            [fail] callback to call if there is a failure
         */
        __load: __load,

        /**
         * Loads jQuery and D3 as a dependencies and then calls the appripriate
         * callback.
         *
         * @access private
         * @param {function}
         *            [success] called if the core dependencies are loaded
         * @param {function}
         *            [fail] called if the core dependencies are not loaded
         */
        __bootstrap: __bootstrap
    };

})();
