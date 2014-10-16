(function(){

// various thingies
var visualization,
	utils,
	Chart,
	debug,
	dependency;
/**
 * polyfills.js
 * Polyfill any required javascript features that are missing
 */
(function(){
	// make sure that Array.forEach is available
    if (!('forEach' in Array.prototype)) {
        Array.prototype.forEach= function(action, that /*opt*/) {
            for (var i= 0, n= this.length; i<n; i++) {
                if (i in this) {
                    action.call(that, this[i], i, this);
                }
            }
        };
    }

    if (typeof String.prototype.endsWith !== 'function') {
        String.prototype.endsWith = function(suffix) {
            return this.indexOf(suffix, this.length - suffix.length) !== -1;
        };
    }

    if (typeof String.prototype.startsWith !== 'function') {
        String.prototype.startsWith = function(str) {
            return this.slice(0, str.length) === str;
        };
    }

})();
/**
 * utils.js
 * utility functions
 */
(function(){
    "use strict";

    utils = {

        /**
         * Deep object merge. This merge differs significantly from the "extend"
         * method provide by jQuery in that it will merge the value of arrays, but
         * concatenating the arrays together using the jQuery method "merge".
         * Neither of the objects passed are modified and a new object is returned.
         *
         * @access private
         * @param {object}
         *            base the object to which values are to be merged into
         * @param {object}
         *            extend the object from which values are merged
         * @param {bool}
         *            tells merge to overwrite arrays instead of concat
         * @returns {object} the merged object
         */
        __merge: function __merge(base, extend, overwriteArrays) {
            var m, k, v;
            if (debug.debug) {
                debug.__groupCollapsed('Object Merge');
                debug.__group('SOURCES');
                debug.__log(base);
                debug.__log(extend);
                debug.__groupEnd();
            }

            if (base === undefined || base === null) {
                m = $.extend(true, {}, extend);
                if (debug.debug) {
                    debug.__log(m);
                    debug.__groupEnd();
                }
                return m;
            }
            if (extend === undefined || extend === null) {
                m = $.extend(true, {}, base);
                if (debug.debug) {
                    debug.__log(m);
                    debug.__groupEnd();
                }
                return m;
            }

            m = $.extend(true, {}, base);
            for (k in extend) {
                if (extend.hasOwnProperty(k)) {
                    v = extend[k];
                    if(v === null || v === undefined){
                        m[k] = v;
                    } else if (v.constructor === Number || v.constructor === String) {
                        m[k] = v;
                    } else if (v instanceof Array) {
                        if(overwriteArrays){
                            m[k] = v;
                        } else {
                            m[k] = $.merge(m[k], v);
                        }
                    } else if (v instanceof Object) {
                        if (m[k] === undefined) {
                            m[k] = $.extend({}, v);
                        } else {
                            m[k] = __merge(m[k], v);
                        }
                    } else {
                        m[k] = $.extend(m[k], v);
                    }
                }
            }

            if (debug.debug) {
                debug.__log(m);
                debug.__groupEnd();
            }
            return m;
        },

        Error: function(name, message) {
            this.name = name;
            this.message = message;
            this.toString = function() {
                return this.name + ' : ' + this.message;
            };
        }
    };

})();
/**
 * debug.js
 * debug/logging utils
 */
(function(){
    "use strict";

    debug = {
        
        /**
         * Used to enable (true) or disable (false) debug output to the
         * browser console
         *
         * @access public
         * @default false
         */
        debug: false,
        
        /**
         * Wrapper around the console group function. This wrapper protects
         * the client from those browsers that don't support the group
         * function.
         *
         * @access private
         */
        __group: function() {
            if (console !== undefined) {
                if (console.group !== undefined) {
                    console.group.apply(console, arguments);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // Oh well
            }
        },

        /**
         * Wrapper around the console groupCollapsed function. This wrapper
         * protects the client from those browsers that don't support this
         * function.
         *
         * @access private
         */
        __groupCollapsed: function() {
            if (console !== undefined) {
                if (console.groupCollapsed !== undefined) {
                    console.groupCollapsed.apply(console, arguments);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // Oh well
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __groupEnd: function() {
            if (console !== undefined) {
                if (console.groupEnd !== undefined) {
                    console.groupEnd.apply(console, arguments);
                } else if (console.log !== undefined) {
                    console.log.apply(console, [ "END" ]);
                }
                // Oh well
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __error: function() {
            if (console !== undefined) {
                if (console.error !== undefined) {
                    console.error(arguments[0]);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // If neither of those exists, oh well ....
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __warn: function() {
            if (console !== undefined) {
                if (console.warn !== undefined) {
                    console.warn(arguments[0]);
                } else if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // If neither of those exists, oh well ....
            }
        },

        /**
         * Wrapper around the console function. This wrapper protects the
         * client from those browsers that don't support this function.
         *
         * @access private
         */
        __log: function() {
            if (console !== undefined) {
                if (console.log !== undefined) {
                    console.log.apply(console, arguments);
                }
                // Oh well
            }
        }
    };
})();
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
                source: "moment-timezone-data.js",
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
/**
 * visualization.js
 * create main visualization config object
 */
(function(){
    "use strict";

    // indicates if the base dependencies (stuff like
    // jquery, d3, etc) have been loaded
    var depsLoaded = false;

    /**
     * @memberOf zenoss
     * @namespace
     * @access public
     */
    visualization = {

        /**
         * Used to specify the base URL that is the endpoint for the Zenoss
         * metric service.
         *
         * @access public
         * @default http://localhost:8080
         */
        url: "http://localhost:8080",

        /**
         * The url path where the static javascript dependencies can be
         * found. This includes library dependencies like jquery.
         *
         * @access public
         * @default /static/performance/query
         */
        urlPath: "/static/performance/query/",

        /**
         * The url path where metrics are fetched from the server
         *
         * @access public
         * @default /api/performance/query
         */
        urlPerformance: "/api/performance/query/",
        

        /**
         * @namespace
         * @access public
         */
        chart : {
            /**
             * Looks up a chart instance by the given name and, if found,
             * updates the chart instance with the given changes. To remove
             * an item (at the first level or the change structure) set its
             * values to the negative '-' symbol.
             *
             * @param {string}
             *            name the name of the chart to update
             * @param {object}
             *            changes a configuration object that holds the
             *            changes to the chart
             */
            update : function(name, changes) {
                var found = getChart(name);
                if (found === undefined) {
                    debug.__warn('Attempt to modify a chart, "' + name +
                        '", that does not exist.');
                    return;
                }
                found.update(changes);
            },

            /**
             * Constructs a zenoss.visualization.Chart object, but first
             * dynamically loading any chart definition required, then
             * dynamically loading all dependencies, and finally creating
             * the chart object. This method should be used to create a
             * chart as opposed to calling "new" directly on the class.
             *
             * @param {string}
             *            name the name of the HTML div element to augment
             *            with the chart
             * @param {string}
             *            [template] the name of the chart template to load.
             *            The chart template will be looked up as a resource
             *            against the Zenoss metric service.
             * @param {object}
             *            [config] the values specified as the configuration
             *            will augment / override options loaded from any
             *            chart template that is specified, thus if no chart
             *            template is specified this configuration parameter
             *            can be used to specify the entire chart
             *            definition.
             * @param {callback}
             *            [success] this callback will be called when a
             *            zenoss.visualization.Chart object is successfully
             *            created. The reference to the Chart object will be
             *            passed as a parameter to the callback.
             * @param {callback}
             *            [fail] this callback will be called when an error
             *            is encountered during the creation of the chart.
             *            The error that occurred will be passed as a
             *            parameter to the callback.
             */
            create : function(name, config) {

                if (!depsLoaded) {
                    dependency.__bootstrap(function() {
                        depsLoaded = true;
                        cacheChart(new Chart(name, config));
                    });
                    return;
                }

                cacheChart(new Chart(name, config));
            },

            // expose chart cache getter
            getChart: getChart
        }
    };


    // chart cache with getter/setters
    var chartCache = {};

    function cacheChart(chart){
        var numCharts;

        chartCache[chart.name] = chart;

        // automatically remove this chart
        // if the containing dom element
        // is destroyed
        chart.onDestroyed = function(e){
            removeChart(chart.name);
        };

        // if there are many charts in here
        // this could indicate a problem
        numCharts = Object.keys(chartCache).length;
        if(numCharts > 12){
            console.warn("There are", numCharts, "cached charts. This can lead to performance issues.");
        }
    }

    function removeChart(name){
        delete chartCache[name];
    }

    function getChart(name){
        return chartCache[name];
    }

})();
/**
 * Chart.js
 * main chart object
 */
(function(){
    "use strict";

    /**
     * This class should not be instantiated directly unless the caller
     * really understand what is going on behind the scenes as there is
     * a lot of concurrent processing involved as many components are
     * loaded dynamically with a delayed creation or realization.
     *
     * Instead instance of this class are better created with the
     * zenoss.visualization.chart.create method.
     *
     * @access private
     * @constructor
     * @param {string}
     *            name the name of the HTML div element to augment with
     *            the chart
     * @param {object}
     *            config the values specified as the configuration will
     *            augment / override options loaded from any chart
     *            template that is specified, thus if no chart template
     *            is specified this configuration parameter can be used
     *            to specify the entire chart definition.
     */

    var DEFAULT_NUMBER_FORMAT = "%4.2f";

    // data for formatting time ranges
    var TIME_DATA = [
        {
            name: "minute",
            value: 60000,
            ticks: 4,
            // max number of units before we should go up a level
            breakpoint: 90,
            format: function(tz, d){ return moment.utc(d).tz(tz).format("HH:mm:ss"); }
        },{
            name: "hour",
            value: 3.6e+6,
            ticks: 4,
            breakpoint: 20,
            format: function(tz, d){ return moment.utc(d).tz(tz).format("HH:mm:ss"); }
        },{
            name: "day",
            value: 8.64e+7,
            ticks: 3,
            breakpoint: 7,
            format: function(tz, d){ return moment.utc(d).tz(tz).format("MM/DD/YY HH:mm:ss"); }
        },{
            name: "week",
            value: 6.048e+8,
            ticks: 3,
            breakpoint: 4,
            format: function(tz, d){ return moment.utc(d).tz(tz).format("MM/DD/YY HH:mm:ss"); }
        },{
            name: "month",
            value: 2.63e+9,
            ticks: 3,
            breakpoint: 13,
            format: function(tz, d){ return moment.utc(d).tz(tz).format("MM/DD/YY HH:mm:ss"); }
        },{
            name: "year",
            value: 3.156e+10,

            ticks: 3,
            breakpoint: 1000,
            format: function(tz, d){ return moment.utc(d).tz(tz).format("MM/DD/YY HH:mm:ss"); }
        }
    ];

    Chart = function(name, config) {
        this.name = name;
        this.config = config;
        this.yAxisLabel = config.yAxisLabel;

        this.$div = $('#' + this.name);

        // listen for the container div to be removed and
        // call cleanup method
        this.$div.on("DOMNodeRemovedFromDocument", this.__onDestroyed.bind(this));

        if (!this.$div.length) {
            throw new utils.Error('SelectorError', 'unknown selector specified, "' + this.name + '"');
        }

        // base should be something like 1000 or 1024
        this.base = config.base || 1000;

        // Build up a map of metric name to legend label.
        this.__buildPlotInfo();

        this.overlays = config.overlays || [];
        // set the format or a default
        this.format = config.format || DEFAULT_NUMBER_FORMAT;
        if ($.isNumeric(config.miny)) {
            this.miny = config.miny;
        }
        if ($.isNumeric(config.maxy)) {
            this.maxy = config.maxy;
        }
        this.timezone = config.timezone || jstz.determine().name();
        this.svgwrapper = document.createElement('div');
        $(this.svgwrapper).addClass('zenchart');
        this.$div.append($(this.svgwrapper));
        this.containerSelector = '#' + name + ' .zenchart';

        this.message = document.createElement('div');
        $(this.message).addClass('message');
        $(this.message).css('display', 'none');
        this.$div.append($(this.message));

        this.footer = document.createElement('div');
        $(this.footer).addClass('zenfooter');
        this.$div.append($(this.footer));

        this.svg = d3.select(this.svgwrapper).append('svg');
        try {
            this.request = this.__buildDataRequest(this.config);

            if (debug.debug) {
                debug.__groupCollapsed('POST Request Object');
                debug.__log(visualization.url + visualization.urlPerformance);
                debug.__log(this.request);
                debug.__groupEnd();
            }

            /*
             * Sanity Check. If the request contained no metrics to
             * query then log this information as a warning, as it
             * really does not make sense.
             */
            if (this.request.metrics === undefined) {
                debug.__warn('Chart configuration contains no metric sepcifications. No data will be displayed.');
            }
            this.update();
        } catch (x) {
            debug.__error(x);
            this.__showError(x);
        }
    };

    Chart.prototype = {
        constructor: Chart,

        __onDestroyed: function(e){
            // check if the removed element is the chart container
            if(this.$div[0] === e.target){
                if(typeof this.onDestroyed === "function"){
                    this.onDestroyed.call(this, e);
                }
            }
        },

        /**
         * Formats the given value according to the format specified by the
         * configuration or a default and returns the result.
         *
         * @access private
         * @param {number}
         *            The number we are formatting
         * @param {string}
         *            The format string for example "%2f";
         */
        formatValue: function(value) {
            /*
             * If we were given a undefined value, Infinity, of NaN (all things that
             * can't be formatted, then just return the value.
             */
            if (!$.isNumeric(value)) {
                return value;
            }

            return toEng(value, this.preferredYUnit, this.format, this.base);
        },

        /**
         * Iterates over the list of data plots and sets up display information
         * about each plot, including its legend label, color, and if it is filled
         * or not.
         *
         * @access private
         */
        __buildPlotInfo: function() {
            var i, info, dp;

            this.plotInfo = {};
            for (i in this.config.datapoints) {
                dp = this.config.datapoints[i];
                info = {
                    'legend' : dp.legend || dp.name || dp.metric,
                    'color' : dp.color,
                    'fill' : dp.fill
                };
                this.plotInfo[dp.name || dp.metric] = info;
            }
        },

        /**
         * Checks to see if the passed in plot is actually an overlay.
         *
         * @access private
         * @param {object}
         *            plot the object representing the plot
         * @return boolean if the plot is an overlay
         */
        __isOverlay: function(plot) {
            var i, key = (typeof plot === 'string' ? plot : plot.key);
            if (this.overlays.length) {
                for (i = 0; i < this.overlays.length; i += 1) {
                    if (this.overlays[i].legend === key) {
                        return true;
                    }
                }
            }
            return false;
        },

        /**
         * Set the relative size of the chart and footer, if configured for a
         * footer, and then resizes the underlying chart.
         *
         * @access private
         */
        __resize: function() {
            var fheight, height, span;

            fheight = this.__hasFooter() ? parseInt($(this.table).outerHeight(), 10)
                    : 0;
            height = parseInt(this.$div.height(), 10) - fheight;
            span = $(this.message).find('span');

            $(this.svgwrapper).outerHeight(height);
            if (this.impl) {
                this.impl.resize(this, height);
            }

            $(this.message).outerHeight(height);
            span.css('margin-top', -parseInt(span.height(), 10) / 2);
        },

        /**
         * Constructs and appends a footer row onto the footer table
         *
         * @access private
         */
        __appendFooterRow: function() {
            var tr, td, d, i;

            tr = document.createElement('tr');
            $(tr).addClass('zenfooter_value_row');

            // One column for the color
            td = document.createElement('td');
            $(td).addClass('zenfooter_box_column');
            d = document.createElement('div');
            $(d).addClass('zenfooter_box');
            $(d).css('backgroundColor', 'white');
            $(td).append($(d));
            $(tr).append($(td));

            // One column for the metric name
            td = document.createElement('td');
            $(td).addClass('zenfooter_data');
            $(td).addClass('zenfooter_data_text');
            $(tr).append($(td));

            // One col for each of the metrics stats
            for (i = 0; i < 4; i += 1) {
                td = document.createElement('td');
                $(td).addClass('zenfooter_data');
                $(td).addClass('zenfooter_data_number');
                $(tr).append($(td));
            }

            $(this.table).append($(tr));
            return $(tr);
        },

        __getAssociatedPlot: function(dp) {
            var i, ll;
            if (!this.plots) {
                return undefined;
            }

            ll = this.plots.length;
            for (i = 0; i < ll; i += 1) {
                if (this.plots[i].key === (dp.legend || dp.metric)) {
                    return this.plots[i];
                }
            }
            return undefined;
        },

        /**
         * Updates the chart footer based on updated data. This includes adding or
         * removing footer rows as well as filling in colors and data.
         *
         * @access private
         * @return true if the changes to the footer necesitates a resize of the
         *         chart, else false.
         */
        __updateFooter: function(data) {
            var sta, eta, plot, dp, vals, cur, min, max, avg, cols, init, label, ll, i, v, vIdx, k, rows, row, box, color, resize = false,
                timezone = this.timezone;
            if (!this.table) {
                return false;
            }
            rows = $(this.table).find('tr');
            if (data) {
                sta = this.dateFormatter(data.startTimeActual, timezone );
                eta = this.dateFormatter(data.endTimeActual, timezone);
            } else {
                sta = eta = "N/A";
            }
            $($(rows[0]).find('td')).html(
                    sta + ' to ' + eta + ' (' + timezone + ')');

            /*
             * The class on the value rows was set when they were created so get a
             * list of all those.
             */
            rows = $(this.table).find('tr.zenfooter_value_row');

            /*
             * Calculate the summary values from the data and place the date in the
             * the table.
             */
            ll = this.config.datapoints.length;
            row = 0;
            if (!this.__footerRangeOnly()) {
                for (i in this.config.datapoints) {
                    dp = this.config.datapoints[i];
                    plot = this.__getAssociatedPlot(dp);
                    if (!this.__isOverlay(dp.legend || dp.metric) &&
                            (dp.emit === undefined || dp.emit)) {
                        if (row >= rows.length) {
                            rows.push(this.__appendFooterRow());
                            resize = true;
                        }

                        // The first column is the color, the second is the metric
                        // name,
                        // followed byt the values
                        cols = $(rows[row]).find('td');

                        // footer color
                        if (this.impl) {
                            color = this.impl.color(this, this.closure, i);
                        } else {
                            // unable to determine color
                            color = {
                                color: "white",
                                opacity: 1
                            };
                        }

                        if (dp.color) {
                            color.color = dp.color;
                        }
                        box = $(cols[0]).find('div.zenfooter_box');
                        box.css('background-color', color.color);
                        box.css('opacity', color.opacity);

                        // Metric name
                        label = dp.legend || dp.metric;
                        if ((k = label.indexOf('{')) > -1) {
                            label = label.substring(0, k) + '{*}';
                        }

                        $(cols[1]).html(label);
                        // we purposefully put two null points so that the graph still renders
                        if (!plot || (plot.values.length == 2 && plot.values[0].y === null && plot.values[1].y === null )) {
                            // communicate to the user that this plot has no v
                            $(cols[1]).html(label + " (<em>No Data Available</em>)");
                            for (v = 2; v < 6; v += 1) {
                                $(cols[v]).html('N/A');
                            }
                        } else {
                            vals = [ 0, 0, 0, 0 ];
                            cur = 0;
                            min = 1;
                            max = 2;
                            avg = 3;
                            init = false;

                            for (vIdx in plot.values) {
                                v = plot.values[vIdx];
                                // don't attempt to calculate nulls
                                if (v.y === null) {
                                    continue;
                                }
                                if (!init) {
                                    vals[min] = v.y;
                                    vals[max] = v.y;
                                    init = true;
                                } else {
                                    vals[min] = Math.min(vals[min], v.y);
                                    vals[max] = Math.max(vals[max], v.y);
                                }
                                vals[avg] += v.y;
                                vals[cur] = v.y;
                            }
                            vals[avg] = vals[avg] / plot.values.length;
                            for (v = 0; v < vals.length; v += 1) {
                                $(cols[2 + v]).html(this.formatValue(vals[v]));
                            }
                        }
                        row += 1;
                    }
                }
            }

            // Extra rows exit in the table and need to be remove
            if (row < rows.length - 1) {
                for (i = rows.length - 1; i >= row; i -= 1) {
                    rows[i].remove();
                }
                resize = true;
            }
            return resize;
        },

        /**
         * Returns true if this chart is displaying a footer, else false
         *
         * @access private
         * @return true if this chart is displaying a footer, else false
         */
        __hasFooter: function() {
            return (this.config.footer === undefined ||
                (typeof this.config.footer === 'boolean' && this.config.footer === true) ||
                (typeof this.config.footer === 'string' && this.config.footer === 'range'));
        },

        /**
         * Returns true if this chart is displaying only the range in the footer,
         * else false
         *
         * @access private
         * @return true if this chart is displaying only the range in the footer,
         *         else false
         */
        __footerRangeOnly: function() {
            return (typeof this.config.footer === 'string' && this.config.footer === 'range');
        },

        /**
         * Constructs the chart footer for a given chart. The footer will contain
         * information such as the date range and key values (ending, min, max, avg)
         * of each plot on the chart.
         *
         * @access private
         * @param {object}
         *            config the charts configuration
         * @param {object}
         *            data the data returned from the metric service that contains
         *            the data to be charted
         */
        __buildFooter: function(config, data) {
            var tr, td, dates, th;
            this.table = document.createElement('table');
            $(this.table).addClass('zenfooter_content');
            $(this.table).addClass('zenfooter_text');
            $(this.footer).append($(this.table));

            // One row for the date range of the chart
            tr = document.createElement('tr');
            td = document.createElement('td');
            dates = document.createElement('span');
            $(td).addClass('zenfooter_dates');
            $(td).attr('colspan', 6);
            $(dates).addClass('zenfooter_dates_text');
            $(tr).append($(td));
            $(td).append($(dates));
            $(this.table).append($(tr));

            if (!this.__footerRangeOnly()) {


                // One row for the stats table header
                tr = document.createElement('tr');
                tr.innerHTML = '<th class="footer_header zenfooter_box_column"></th>'+
                    '<th class="footer_header zenfooter_data_text">Metric</th>'+
                    '<th class="footer_header zenfooter_data_number">Last</th>'+
                    '<th class="footer_header zenfooter_data_number">Min</th>'+
                    '<th class="footer_header zenfooter_data_number">Max</th>'+
                    '<th class="footer_header zenfooter_data_number">Avg</th>';
                $(this.table).append($(tr));
            }

            // Fill in the stats table
            this.__updateFooter(data);
        },

        /**
         * Updates a graph with the changes specified in the given change set. To
         * remove a value from the configuration its value should be set to a
         * negative sign, '-'.
         *
         * @param {object}
         *            changeset updates to the existing graph's configuration.
         */
        update: function(changeset) {
            var self = this, kill = [], property;

            // This function is really meant to only handle given types of changes,
            // i.e. we don't expect that you can change the type of the graph but
            // you
            // should be able to change the date range.
            this.config = utils.__merge(this.config, changeset, true);

            // A special check for the removal of items from the config. If the
            // value
            // for any item in the change set is '-', then we delete that key.
            for (property in this.config) {
                if (this.config.hasOwnProperty(property)) {
                    if (this.config[property] === '-') {
                        kill.push(property);
                    }
                }
            }
            kill.forEach(function(p) {
                delete self.config[p];
            });

            /*
             * Rebuild the legend and color tables
             */
            this.__buildPlotInfo();

            try {
                this.request = this.__buildDataRequest(this.config);
                $.ajax({
                    'url' : visualization.url + visualization.urlPerformance,
                    'type' : 'POST',
                    'data' : JSON.stringify(this.request),
                    'dataType' : 'json',
                    'contentType' : 'application/json',
                    'success' : function(data) {
                        self.plots = self.__processResult(self.request, data);

                        /*
                         * If the chart has not been created yet, then
                         * create it, else just update the data.
                         */
                        if (!self.closure) {
                            if (self.config.type === undefined) {
                                self.config.type = 'line';
                            }
                            self.__render(data);
                        } else {
                            self.__updateData(data);
                        }

                        // Update the footer
                        if (self.__updateFooter(data)) {
                            self.__resize();
                        }

                        // setPreffered y unit (k, G, M, etc)
                        self.setPreferredYUnit(data.results);

                    },
                    'error' : function(res) {
                        self.plots = undefined;

                        self.__showNoData();
                        // upon errors still show the footer
                        if (self.showLegendOnNoData && self.__hasFooter()) {
                            // if this is the first request that errored we will need to build
                            // the table
                            if (!self.table) {
                                self.__buildFooter(self.config);
                            } else {
                                if (self.__updateFooter()) {
                                    self.__resize();
                                }
                            }
                        }
                    }
                });
            } catch (x) {
                this.plots = undefined;
                if (self.__updateFooter()) {
                    self.__resize();
                }
                debug.__error(x);
                this.__showError(x);
            }
        },

        /**
         * Constructs a request object that can be POSTed to the Zenoss Data API to
         * retrieve the data for a chart. The request is based on the information in
         * the given config.
         *
         * @access private
         * @param {object}
         *            config the config from which to build a request
         * @returns {object} a request object that can be POST-ed to the Zenoss
         *          performance metric service
         */
        __buildDataRequest: function(config) {
            var request = {};
            if (config !== undefined) {
                if (config.range !== undefined) {
                    if (config.range.start !== undefined) {
                        request.start = config.range.start;
                    }
                    if (config.range.end !== undefined) {
                        request.end = config.range.end;
                    }
                }

                request.series = true;
                // series should always be true
                // if (config.series !== undefined) {
                //     request.series = config.series;
                // }

                if (config.downsample !== undefined) {
                    request.downsample = config.downsample;
                }

                if (config.tags !== undefined) {
                    request.tags = config.tags;
                }

                if (config.returnset !== undefined) {
                    request.returnset = config.returnset;
                }

                if (config.datapoints !== undefined) {
                    request.metrics = [];
                    config.datapoints
                            .forEach(function(dp) {
                                var m = {}, key, expressionMetric;
                                if (dp.metric !== undefined) {
                                    m.metric = dp.metric;

                                    if (dp.rate !== undefined) {
                                        m.rate = dp.rate;
                                    }
                                    if (dp.rateOptions !== undefined && dp.rateOptions !== null) {
                                        m.rateOptions = dp.rateOptions;
                                    }
                                    if (dp.aggregator !== undefined) {
                                        m.aggregator = dp.aggregator;
                                    }

                                    if (dp.tags !== undefined) {
                                        m.tags = {};
                                        for (key in dp.tags) {
                                            if (dp.tags.hasOwnProperty(key)) {
                                                m.tags[key] = dp.tags[key];
                                            }
                                        }
                                    }

                                    if (dp.emit === false) {
                                        m.emit = false;
                                    }

                                    if (dp.name === undefined) {
                                        m.name = dp.metric;
                                    } else {
                                        m.name = dp.name;
                                    }
                                } else if (dp.name !== undefined) {
                                    m.name = dp.name;
                                } else {
                                    /*
                                     * This data point has neither a metric
                                     * definition nor a name (virtual metric)
                                     * deffined. As such this is an invalid
                                     * specification. Because of this we will fail
                                     * the entire request so that the caller is not
                                     * confused as to why partial data is returned.
                                     */
                                    throw sprintf(
                                        "Invalid data point specification in request, '%s'. No 'metric' or 'name' attribute specified, failing entire request.",
                                        JSON.stringify(dp, null, ' '));
                                }

                                if (dp.expression) {
                                    expressionMetric = {
                                        name: m.name,
                                        // rewrite the expression to look for the
                                        // renamed datapoint
                                        expression: dp.expression.replace("rpn:", "rpn:"+ m.name + "-rpn,")
                                    };

                                    // original datapoint is now just a vehicle for the
                                    // expression to evaluate against
                                    m.emit = false;
                                    m.name = m.name + "-rpn";
                                }

                                request.metrics.push(m);

                                // if an expressionMetric was created, add to request
                                if(expressionMetric){
                                    request.metrics.push(expressionMetric);
                                }
                            });

                }
            }
            return request;
        },

        /**
         * Processes the result from the Zenoss performance metric query that is in
         * the series format into the data that can be utilized by the chart
         * library.
         *
         * @access private
         * @param {object}
         *            request the request which generated the data
         * @param {object}
         *            data the data object returned from the query
         * @returns {object} the data in the format that can be utilized by the
         *          chart library.
         */
        __processResultAsSeries: function(request, data) {

            var plots = [],
                start = data.startTimeActual,
                end = data.endTimeActual,
                drange = end - start,
                // allowable deviation expected start/end points
                drangeDeviation = drange * 0.2;

            data.results.forEach(function(series){

                var dp, info, key, plot;

                // if series.datapoints is not defined, or there are no points
                if(!series.datapoints || (series.datapoints && !series.datapoints.length)){
                    series.datapoints = [{
                        timestamp: start,
                        value: null
                    },{
                        timestamp: end,
                        value: null
                    }];
                }

                // ensure the series starts at the expected time (or near it at least)
                if(series.datapoints[0].timestamp !== start && series.datapoints[0].timestamp - start > drangeDeviation){
                    series.datapoints.unshift({
                        timestamp: start,
                        value: null
                    });
                }
                // ensure the series ends at the expected time (or near it at least)
                if(series.datapoints[series.datapoints.length-1].timestamp !== end &&
                    (end - series.datapoints[series.datapoints.length-1].timestamp) > drangeDeviation)
                {
                    series.datapoints.push({
                        timestamp: end,
                        value: null
                    });
                }


                // create plots from each datapoint
                info = this.plotInfo[series.metric];
                key = info.legend;
                // TODO - use tags to make key unique
                plot = {
                    'key' : key,
                    'color' : info.color,
                    'fill' : info.fill,
                    'values' : []
                };

                series.datapoints.forEach(function(datapoint){
                    plot.values.push({
                        x : datapoint.timestamp * 1000,
                        // ensure value is a number
                        y : typeof datapoint.value !== "number" ? null : datapoint.value
                    });
                });

                plots.push(plot);

            }.bind(this));

            return plots;
        },

        /**
         * Wrapper function that redirects to the proper implementation to processes
         * the result from the Zenoss performance metric query into the data that
         * can be utilized by the chart library. *
         *
         * @access private
         * @param {object}
         *            request the request which generated the data
         * @param {object}
         *            data the data object returned from the query
         * @returns {object} the data in the format that can be utilized by the
         *          chart library.
         */
        __processResult: function(request, data) {
            var plots, i, overlay, minDate, maxDate, plot, k, firstMetric;

            plots = this.__processResultAsSeries(request, data);

            // add overlays
            if (this.overlays.length && plots.length && plots[0].values.length) {
                for (i in this.overlays) {
                    overlay = this.overlays[i];
                    // get the date range
                    firstMetric = plots[0];
                    plot = {
                        'key' : overlay.legend + "*",
                        'disabled' : true,
                        'values' : [],
                        'color' : overlay.color
                    };
                    minDate = firstMetric.values[0].x;
                    maxDate = firstMetric.values[firstMetric.values.length - 1].x;
                    for (k = 0; k < overlay.values.length; k += 1) {

                        // create a line by putting a point at the start and a point
                        // at the end
                        plot.values.push({
                            x : minDate,
                            y : overlay.values[k]
                        });
                        plot.values.push({
                            x : maxDate,
                            y : overlay.values[k]
                        });
                    }
                    plots.push(plot);
                }
            }

            return plots;
        },

        /**
         * Returns true if the chart has plots and they contain data points, else
         * false.
         *
         * @access private
         */
        __havePlotData: function() {
            var i, ll;

            if (!this.plots || this.plots.length === 0) {
                return false;
            }

            ll = this.plots.length;
            for (i = 0; i < ll; i += 1) {
                if (this.plots[i].values.length > 0) {
                    return true;
                }
            }
            return false;
        },

        /**
         * Updates the chart with a new data set
         *
         * @access private
         * @param {object}
         *            the new data to display in the chart
         */
        __updateData: function(data) {

            if (!this.__havePlotData()) {
                this.__showNoData();
            } else {
                this.__showChart();
                this.impl.update(this, data);
            }

            if (this.__updateFooter(data)) {
                this.__resize();
            }
        },

        /**
         * Constructs a chart from the given data
         *
         * @param data
         *            the data returned from a metric query
         * @access private
         */
        __buildChart: function(data) {
            $(this.svgwrapper).outerHeight(
                    this.$div.height() - $(this.footer).outerHeight());
            this.closure = this.impl.build(this, data);
            this.impl.render(this);

            // If there is not data, let the user know
            if (!this.__havePlotData()) {
                this.__showNoData();
            }
        },

        /**
         * Loads the chart renderer as a dependency and then constructs and renders
         * the chart.
         *
         * @access private
         * @param {object}
         *            data the data that is being rendered in the graph
         */
        __render: function(data) {
            var self = this;
            dependency.__loadDependencies({
                    'defined' : self.config.type.replace('.', '_'),
                    'source' : [ 'charts/' + self.config.type.replace('.', '/') + '.js' ]
                }, function() {
                    var impl;
                    try {
                        impl = visualization.chart;
                        self.config.type.split('.').forEach(function(seg) {
                            impl = impl[seg];
                        });
                        self.impl = impl;
                    } catch (err) {
                        throw new utils.Error(
                            'DependencyError',
                            'Unable to locate loaded chart type, "' +
                                self.config.type + '", error: ' + err
                        );
                    }

                    // Check the impl to see if a dependency is listed
                    // and
                    // if so load that.
                    dependency.__loadDependencies(self.impl.required, function() {
                        self.__buildChart(data);
                        if (self.__hasFooter()) {
                            self.__buildFooter(self.config, data);
                        }
                        self.__resize();
                    });
            });
        },

        /**
         * Used to augment the div element with an error message when an
         * error is encountered while creating a chart.
         *
         * @access private
         * @param {string}
         *            name the ID of the HTML div element to augment
         * @param {object}
         *            err the error object
         * @param {string}
         *            detail the detailed error message
         */
        __showError: function(detail) {
            this.__showMessage('<span class="zenerror">' + detail + '</span>');
        },

        /**
         * Shows a no data available message in the chart and hides any
         * chart elements such as the chart and the footer.
         *
         * @access private
         * @param {string}
         *            name of the div wrapper for the chart
         */
        __showNoData: function() {
            this.__showMessage('<span class="nodata"></span>');
        },

        __hideMessage: function() {
            this.$div.find(".message").css('display', 'none');
        },

        __showMessage: function(message) {
            // cache some commonly used selectors
            var $message = this.$div.find(".message"),
                $messageSpan = $message.find("span");

            if (message) {
                $message.html(message);
            }
            this.__hideChart();

            $message.css('display', 'block');
        },

        __hideChart: function() {
            this.$div.find('.zenchart').css('display', 'none');

            if (!this.showLegendOnNoData) {
                this.$div.find('.zenfooter').css('display', 'none');
            }
        },

        __showChart: function() {
            this.__hideMessage();
            this.$div.find('.zenchart').css('display', 'block');
            this.$div.find('.zenfooter').css('display', 'block');
        },

        /**
         * Used to format dates for the output display in the footer of a
         * chart.
         *
         * @param {int}
         *            unix timestamp of the date to be formated
         * @returns a string representation of the date
         * @access public
         */
        dateFormatter: function(date, timezone) {
            return moment.utc(date, "X").tz(timezone).format(this.dateFormat);
        },

        /**
         * Determines if the legend is displayed when no data is available
         * for any plot in the grid.
         *
         * @access public
         * @default true
         */
        showLegendOnNoData: true,

        /**
         * Used for formatting the date in the legend of the chart.
         * It must be a valid moment.js date format.
         * http://momentjs.com/docs/#/parsing/string-format/
         * @access public
         * @default "MM/DD/YY HH:mm:ss a"
         */
        dateFormat: "MM/DD/YY HH:mm:ss",

        // uses TIME_DATA to determine which time range we care about
        // and format labels representative of that time range
        updateXLabels: function(start, end, axis){
            var dateRange = end - start,
                done, timeFormat;

            // figure out which unit we care about
            TIME_DATA.forEach(function(timeFormatObj){
                if(!done && dateRange <= timeFormatObj.value * timeFormatObj.breakpoint){
                    timeFormat = timeFormatObj;
                    done = true;
                }
            });

            // set number of ticks based on unit
            axis.ticks(timeFormat.ticks)
                .tickFormat(timeFormat.format.bind(null, this.timezone));
        },

        dedupeYLabels: function(model){
            var prevY;

            return function(value, index) {
                var yDomain = model.yDomain() || [0,1],
                    formatted = this.formatValue(value),
                    // min and max labels do not have an index set
                    // where regular labels do
                    isMinMax = index === undefined ? true : false;

                // if prevY hasn't been set yet, this is
                // the first time this has been run, so
                // set it.
                if(prevY === undefined){
                    prevY = this.formatValue(yDomain[0]);
                }

                // if this is not the min/max tick, and matches the previous
                // tick value, the min tick value or the max tick value,
                // do not return a tick value (I'm sure that's crystal
                // clear now)
                if(!isMinMax && (formatted === prevY ||
                   formatted === this.formatValue(yDomain[0]) ||
                   formatted === this.formatValue(yDomain[1])) ){
                    return undefined;

                // if prevY is a unique value, return it
                } else {
                    prevY = formatted;
                    return formatted;
                }
            }.bind(this);
        },

        /**
         * Create y domain based on options and calculated data range
         */
        calculateYDomain: function(miny, maxy, data){
            // if max is not provided, calcuate max
            if(maxy === undefined){
                maxy = this.calculateResultsMax(data.results);
            }

            // if min is not provided, calculate min
            if(miny === undefined){
                miny = this.calculateResultsMin(data.results);
            }

            // if min and max are the same, add a bit to
            // max to separate them
            if(miny === maxy){
                maxy += maxy * 0.1;
            }

            // if min and max are zero, force a
            // 0,1 domain
            if(miny + maxy === 0){
                maxy = 1;
            }

            return [miny, maxy];
        },

        /**
         * Accepts a query service api response and determines the minimum
         * value of all series datapoints in that response
         */
        calculateResultsMin: function(data){
            return data.reduce(function(acc, series){
                return Math.min(acc, series.datapoints.reduce(function(acc, dp){
                    // if the value is the string "NaN", ignore this dp
                    if(dp.value === "NaN") return acc;
                    return Math.min(acc, +dp.value);
                }, 0));
            }, 0);
        },

        /**
         * Accepts a query service api response and determines the maximum
         * value of all series datapoints in that response
         */
        calculateResultsMax: function(data){

            var seriesCalc = function(a,b){
                return a+b;
            };

            return data.reduce(function(acc, series){
                return seriesCalc(acc, series.datapoints.reduce(function(acc, dp){
                    // if the value is the string "NaN", ignore this dp
                    if(dp.value === "NaN") return acc;
                    return Math.max(acc, +dp.value);
                }, 0));
            }, 0);
        },

        setPreferredYUnit: function(data){
            var max = this.calculateResultsMax(data),
                exponent = +max.toExponential().split("e")[1];

            while(exponent % 3){
                exponent--;
            }

            this.preferredYUnit = exponent;
        }
   };

    var SYMBOLS = {
        "-24": "y",
        "-21:": "z",
        "-18": "a",
        "-15": "f",
        "-12": "p",
        "-9": "n",
        "-6": "u",
        "-3": "m",
        "0": "",
        "3": "k",
        "6": "M",
        "9": "G",
        "12": "T",
        "15": "P",
        "18": "E",
        "21": "Z",
        "24": "Y"
    };

    function toEng(val, preferredUnit, format, base){
        var v = val.toExponential().split("e"),
            coefficient = +v[0],
            exponent = +v[1],
            // engineering notation rolls over every 1000 units,
            // and each step towards that is a power of 10, but
            // we may want to roll over on other values, so factor
            // in the provided base value (eg: 1024 for bytes)
            multi = (1000 / base) * 10,
            result = val;

        // if preferredUnit is provided, target that value
        if(preferredUnit !== undefined){
            coefficient *= Math.pow(multi, exponent - preferredUnit);
            exponent = preferredUnit;
        }

        // exponent is not divisible by 3, we got work to do
        while(exponent % 3){
            coefficient *= multi;
            exponent--;
        }

        // divide result by base
        for(var i = 0; i < exponent; i += 3){
           result /= base;
        }

        return sprintf(format, result) + SYMBOLS[exponent];
    }
})();


window.zenoss = {
	visualization: visualization,
	utils: utils,
	Chart: Chart,
	debug: debug,
	dependency: dependency
};

})();