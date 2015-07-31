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
        String.prototype.startsWith = function(prefix) {
            return this.indexOf(prefix) === 0;
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
                            m[k] = __merge(m[k], v, overwriteArrays);
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
        },

        // time conversion utilities
        createDate: createDate,
        relativeTimeToMS: relativeTimeToMS

    };


    // friendly time to ms conversion
    var TIME_UNITS = {
        s: 1000,
        m: 1000 * 60,
        h: 1000 * 60 * 60,
        d: 1000 * 60 * 60 * 24
    };
    // TODO - create regexp based on Oject.keys(TIME_UNITS)
    var timeUnitsRegExp = /[smhd]/;

    // creates a new Date object from the following date formats:
    //      Date object
    //      Date string - "Thu Dec 04 2014 13:24:54 GMT-0600 (CST)"
    //      ms since epoch - 1417721130861
    //      relative measure - "1h-ago"
    function createDate(val){
        var d;

        // if "ago" appears in val, calculate relative time
        if(typeof val === "string" && val.indexOf("ago") !== -1){
            // calculate today minus relative time
            d = moment().subtract(relativeTimeToMS(val), "ms");

        // parse as date object, date string, or ms since epoch
        } else {
            d = moment(val);
        }

        if(d.isValid()){
            return d;
        }
    }


    // takes a relative measure of time like "2s-ago",
    // and convert to milliseconds
    function relativeTimeToMS(val){
        var agoMatch, unitMatch, count, unit;

        // check if "ago" appears in this string
        agoMatch = /ago/.exec(val);
        // TODO - if !agomatch

        unitMatch = timeUnitsRegExp.exec(val);
        // TODO - if !unitMatch

        // get the count portion of the string and cast to number
        count = +val.slice(0, unitMatch.index);

        // get the unit portion of the string
        unit = val.slice(unitMatch.index, agoMatch.index - 1);

        // return count times unit
        return count * TIME_UNITS[unit];
    }

})();

/**
* @license
*
* Regression.JS - Regression functions for javascript
* http://tom-alexander.github.com/regression-js/
* 
* copyright(c) 2013 Tom Alexander
* Licensed under the MIT license.
*
**/

;(function() {
    'use strict';

    var gaussianElimination = function(a, o) {
           var i = 0, j = 0, k = 0, maxrow = 0, tmp = 0, n = a.length - 1, x = new Array(o);
           for (i = 0; i < n; i++) {
              maxrow = i;
              for (j = i + 1; j < n; j++) {
                 if (Math.abs(a[i][j]) > Math.abs(a[i][maxrow]))
                    maxrow = j;
              }
              for (k = i; k < n + 1; k++) {
                 tmp = a[k][i];
                 a[k][i] = a[k][maxrow];
                 a[k][maxrow] = tmp;
              }
              for (j = i + 1; j < n; j++) {
                 for (k = n; k >= i; k--) {
                    a[k][j] -= a[k][i] * a[i][j] / a[i][i];
                 }
              }
           }
           for (j = n - 1; j >= 0; j--) {
              tmp = 0;
              for (k = j + 1; k < n; k++)
                 tmp += a[k][j] * x[k];
              x[j] = (a[n][j] - tmp) / a[j][j];
           }
           return (x);
    };

        var methods = {
            linear: function(data) {
                var sum = [0, 0, 0, 0, 0], n = 0, results = [];

                for (; n < data.length; n++) {
                  if (data[n][1]) {
                    sum[0] += data[n][0];
                    sum[1] += data[n][1];
                    sum[2] += data[n][0] * data[n][0];
                    sum[3] += data[n][0] * data[n][1];
                    sum[4] += data[n][1] * data[n][1];
                  }
                }

                var gradient = (n * sum[3] - sum[0] * sum[1]) / (n * sum[2] - sum[0] * sum[0]);
                var intercept = (sum[1] / n) - (gradient * sum[0]) / n;
              //  var correlation = (n * sum[3] - sum[0] * sum[1]) / Math.sqrt((n * sum[2] - sum[0] * sum[0]) * (n * sum[4] - sum[1] * sum[1]));

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], data[i][0] * gradient + intercept];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(gradient*100) / 100 + 'x + ' + Math.round(intercept*100) / 100;

                return {equation: [gradient, intercept], points: results, string: string};
            },

            exponential: function(data) {
                var sum = [0, 0, 0, 0, 0, 0], n = 0, results = [];

                for (len = data.length; n < len; n++) {
                  if (data[n][1]) {
                    sum[0] += data[n][0];
                    sum[1] += data[n][1];
                    sum[2] += data[n][0] * data[n][0] * data[n][1];
                    sum[3] += data[n][1] * Math.log(data[n][1]);
                    sum[4] += data[n][0] * data[n][1] * Math.log(data[n][1]);
                    sum[5] += data[n][0] * data[n][1];
                  }
                }

                var denominator = (sum[1] * sum[2] - sum[5] * sum[5]);
                var A = Math.pow(Math.E, (sum[2] * sum[3] - sum[5] * sum[4]) / denominator);
                var B = (sum[1] * sum[4] - sum[5] * sum[3]) / denominator;

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], A * Math.pow(Math.E, B * data[i][0])];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(A*100) / 100 + 'e^(' + Math.round(B*100) / 100 + 'x)';

                return {equation: [A, B], points: results, string: string};
            },

            logarithmic: function(data) {
                var sum = [0, 0, 0, 0], n = 0, results = [];

                for (len = data.length; n < len; n++) {
                  if (data[n][1]) {
                    sum[0] += Math.log(data[n][0]);
                    sum[1] += data[n][1] * Math.log(data[n][0]);
                    sum[2] += data[n][1];
                    sum[3] += Math.pow(Math.log(data[n][0]), 2);
                  }
                }

                var B = (n * sum[1] - sum[2] * sum[0]) / (n * sum[3] - sum[0] * sum[0]);
                var A = (sum[2] - B * sum[0]) / n;

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], A + B * Math.log(data[i][0])];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(A*100) / 100 + ' + ' + Math.round(B*100) / 100 + ' ln(x)';

                return {equation: [A, B], points: results, string: string};
            },

            power: function(data) {
                var sum = [0, 0, 0, 0], n = 0, results = [];

                for (len = data.length; n < len; n++) {
                  if (data[n][1]) {
                    sum[0] += Math.log(data[n][0]);
                    sum[1] += Math.log(data[n][1]) * Math.log(data[n][0]);
                    sum[2] += Math.log(data[n][1]);
                    sum[3] += Math.pow(Math.log(data[n][0]), 2);
                  }
                }

                var B = (n * sum[1] - sum[2] * sum[0]) / (n * sum[3] - sum[0] * sum[0]);
                var A = Math.pow(Math.E, (sum[2] - B * sum[0]) / n);

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], A * Math.pow(data[i][0] , B)];
                    results.push(coordinate);
                }

                 var string = 'y = ' + Math.round(A*100) / 100 + 'x^' + Math.round(B*100) / 100;

                return {equation: [A, B], points: results, string: string};
            },

            polynomial: function(data, order) {
                if(typeof order == 'undefined'){
                    order = 2;
                }
                 var lhs = [], rhs = [], results = [], a = 0, b = 0, i = 0, k = order + 1;

                        for (; i < k; i++) {
                           for (var l = 0, len = data.length; l < len; l++) {
                              if (data[l][1]) {
                               a += Math.pow(data[l][0], i) * data[l][1];
                              }
                            }
                            lhs.push(a), a = 0;
                            var c = [];
                            for (var j = 0; j < k; j++) {
                               for (var l = 0, len = data.length; l < len; l++) {
                                  if (data[l][1]) {
                                   b += Math.pow(data[l][0], i + j);
                                  }
                                }
                                c.push(b), b = 0;
                            }
                            rhs.push(c);
                        }
                rhs.push(lhs);

               var equation = gaussianElimination(rhs, k);

                    for (var i = 0, len = data.length; i < len; i++) {
                        var answer = 0;
                        for (var w = 0; w < equation.length; w++) {
                            answer += equation[w] * Math.pow(data[i][0], w);
                        }
                        results.push([data[i][0], answer]);
                    }

                    var string = 'y = ';

                    for(var i = equation.length-1; i >= 0; i--){
                      if(i > 1) string += Math.round(equation[i]*100) / 100 + 'x^' + i + ' + ';
                      else if (i == 1) string += Math.round(equation[i]*100) / 100 + 'x' + ' + ';
                      else string += Math.round(equation[i]*100) / 100;
                    }

                return {equation: equation, points: results, string: string};
            },

            lastvalue: function(data) {
              var results = [];
              var lastvalue = null;
              for (var i = 0; i < data.length; i++) {
                if (data[i][1]) {
                  lastvalue = data[i][1];
                  results.push([data[i][0], data[i][1]]);
                }
                else {
                  results.push([data[i][0], lastvalue]);
                }
              }

              return {equation: [lastvalue], points: results, string: "" + lastvalue};
            }
        };

var regression = (function(method, data, order) {

       if (typeof method == 'string') {
           return methods[method](data, order);
       }
    });

if (typeof exports !== 'undefined') {
    module.exports = regression;
} else {
    window.regression = regression;
}

}());

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

    var projectionAlgorithms = {

        /**
         * Uses the supplied values to return a function that accepts
         * an "x" value and returns a "y" value.
         * Library: "regression.js" https://github.com/Tom-Alexander/regression-js
         **/
        linear: function(projection, xValues, yValues) {
            // the regression library an array of x,y values, e.g.  [[x, y], [x1, y1],...]
            var data = [], i, formula, slope, intercept;
            for (i=0;i < xValues.length; i++) {
                data.push([xValues[i], yValues[i]]);
            }
            // return basically an empty function so clients do not error out when calling it
            if (data.length === 0) {
                return function(x){ return 0; };
            }

            // linear regression
            formula = window.regression("linear", data);
            slope = formula.equation[0];
            intercept = formula.equation[1];

            return function(x) {
                // y = mx + b
                return (slope * x) + intercept;
            };
        },
        polynomial: function(projection, xValues, yValues) {
            // the regression library an array of x,y values, e.g.  [[x, y], [x1, y1],...]
            var data = [], i, formula, slopes, intercept, n;
            for (i=0;i < xValues.length; i++) {
                data.push([xValues[i], yValues[i]]);
            }
            // return basically an empty function so clients do not error out when calling it
            if (data.length === 0) {
                return function(x){ return 0; };
            }

            // polynomial regression
            n = parseInt(projection.parameters['n'] || 2);
            formula = window.regression("polynomial", data, n);

            // first entry is the intercept
            intercept = formula.equation.shift();

            slopes = formula.equation;
            // work with the largest number first
            slopes.reverse();
            //equation is something like: "y = 0.1x^2 + 0.2x + -64010.58"
            // where the coeffecients are in the array "slopes"
            return function(x) {
                var currentPow = n, i, result = 0;
                // all the cx^y parts
                for (i=0; i < slopes.length; i++) {
                    result += slopes[i] * (Math.pow(x, currentPow));
                    currentPow--;
                }
                // finally add the intercept
                result += intercept;
                return result;
            };
        }
    };
    visualization.projections = projectionAlgorithms;

    // chart cache with getter/setters
    var chartCache = {};

    function cacheChart(chart){
        chartCache[chart.name] = chart;

        // automatically remove this chart
        // if the containing dom element
        // is destroyed
        chart.onDestroyed = function(e){
            removeChart(chart.name);
        };

        // TODO - watch for stale charts
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

    // downsampling based on range of selection
    var DOWNSAMPLE = [
        // for now when the delta is < 1 hour we do NOT do downsampling
        [3600000, '10s-avg'],     // 1 Hour
        [7200000, '30s-avg'],     // 2 Hours
        [14400000, '45s-avg'],    // 4 Hours
        [18000000, '1m-avg'],     // 5 Hours
        [28800000, '2m-avg'],     // 8 Hours
        [43200000, '3m-avg'],     // 12 Hours
        [64800000, '4m-avg'],     // 18 Hours
        [86400000, '5m-avg'],     // 1 Day
        [172800000, '10m-avg'],   // 2 Days
        [259200000, '15m-avg'],   // 3 Days
        [604800000, '1h-avg'],    // 1 Week
        [1209600000, '2h-avg'],   // 2 Weeks
        [2419200000, '6h-avg'],   // 1 Month
        [9676800000, '1d-avg'],   // 4 Months
        [31536000000, '10d-avg']  // 1 Year
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
        this.projections = config.projections || [];
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
         * @param {ignorePreferred}
         *            If toEng function should ignore the preferred unit
         *            and calculate a unit based on `value`
         */
        formatValue: function(value, ignorePreferred) {
            /*
             * If we were given a undefined value, Infinity, of NaN (all things that
             * can't be formatted, then just return the value.
             */
            if (!$.isNumeric(value)) {
                return value;
            }

            return toEng(value, ignorePreferred ? undefined : this.preferredYUnit, this.format, this.base);
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
         * Returns all the current chart's plots that are of type projection
         *
         * @access private
         * @return [object] all the projection plots
         */
        __getProjectionPlots: function() {
            return $.grep(this.plots, function(p) { return p.projection; } );
        },
        /**
         * Renders the projection legend with a mouse over of future dates
         * @access private
         **/
        __renderProjectionFooter: function() {
            var projections = this.__getProjectionPlots(),
                // the days out that we are showing projections for (e.g. 30 days from now)
                futureTimes = [30, 60, 90];

            // recreate the legend from scratch each update
            $(this.footer).find(".projectionPlots").remove();

            // create the content div.
            $(this.footer).append("<div class='projectionPlots'><span style='font-weight:bold;'>Projections</></div>");
            // get a jquery handle on it
            var div = $(this.footer).find(".projectionPlots");

            // create a new row with
            projections.forEach(function(projection) {
                var table = "<table width='250px'>" +
                    "<tr><th><b>Date</b></th><th><b>Value</b></th></tr>", i, futureTime, uniqueDivId = Math.round(new Date().getTime() + (Math.random() * 100)).toString();
                for (i=0; i< futureTimes.length; i++) {
                    futureTime = moment().add(futureTimes[i], 'days');
                    table += "<tr><td>" + futureTime.format("MMM-D") + " ("  + futureTimes[i].toString() + " days)</td><td align='right'>" +
                        Number(projection.projectionFn(futureTime.unix()).toFixed(2)).toLocaleString('en')  +
                        "</td></tr>";
                }
                table  += "</table>";

                // add a row representing the projection
                div.append('<div id=' + uniqueDivId  +
                           ' title="placeholder"  > <div class="zenfooter_box" style="opacity: 1;">' +
                           '</div><span class="projectionLegend">&nbsp;&nbsp;' + projection.key.replace("Projected ", "") +
                           '</span><div class="info_icon"><span style="font-style: italic">i</span></div></div>');
                $("#" + uniqueDivId + " .zenfooter_box").css("background-color", projection.color);
                // use jQuery UI tool tips to register a table tool tip showing projected values on hover
                $("#" + uniqueDivId).tooltip({
                    show: {
                        effect: "slideDown",
                        delay: 150
                    },
                    content: function() {
                        return table;
                    }
                });
            }.bind(this));

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

                        // setPreffered y unit (k, G, M, etc)
                        self.setPreferredYUnit(data.results);

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
                            // if we have projections wait to render so the chart doesn't jump around
                            if (self.projections === undefined || self.projections.length === 0) {
                                self.__updateData(data);
                            }
                        }

                        // Update the footer
                        if (self.__updateFooter(data)) {
                            self.__resize();
                        }
                        // send a separate request for the projection data since it has a different time span

                        var projectionColors = ["#EBEBEF", "#FDDFE7", "#FCF1C0", "#DAFBEB"], projectionIndex = 0;
                        self.projections.forEach(function(projection) {
                            var projectionRequest = self.__buildProjectionRequest(self.config, self.request, projection);
                            // can fail if the projection is requesting a metric not present
                            if (!projectionRequest) {
                                return;
                            }
                            $.ajax({
                                'url' : visualization.url + visualization.urlPerformance,
                                'type' : 'POST',
                                'data' : JSON.stringify(projectionRequest),
                                'dataType' : 'json',
                                'contentType' : 'application/json',
                                'success' : function(projectionData) {

                                    if (projectionData.results) {
                                        var values = projectionData.results[projectionData.results.length - 1].datapoints || [],
                                            start = utils.createDate(self.request.start || "1h-ago").unix(),
                                            end = utils.createDate(self.request.end || "0s-ago").unix(),
                                            // use strategy to create a return  function that will convert projected X values into Y's
                                            valueFn = self.createRegressionFunction(projection, values),
                                            // get the visible x, y values
                                            projectedSet = self.createRegressionData(valueFn, start, end);
                                        self.plots.push({
                                            color: projectionColors[projectionIndex++ % projectionColors.length],
                                            fill: false,
                                            projection: true,
                                            projectionFn: valueFn,
                                            key: projection.legend,
                                            values: projectedSet
                                        });
                                        // self.closure isn't set because we are still loading dependencies
                                        // wait until the regular chart build is called
                                        if (self.closure) {
                                            self.impl.update(self, data);
                                        }
                                        self.__renderProjectionFooter();
                                    }
                                },
                                'error' : function() {
                                    // the trendline isn't critical to the graph so
                                    // do nothing in the case of errors
                                }
                            });
                        });
                    },
                    'error' : function() {
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
         *  Converts a downsample rate into a "step". To minimized clutter each step is a multiple
         *  the downsample rate.
         **/
        __convertDownsampletoStep: function(downsample) {
            if (!downsample) {
                return 600;
            }
            var regexp = new RegExp(/\d+/),
                numberPart = downsample.split("-")[0],
                number = parseInt(regexp.exec(numberPart)[0]),
                unit = numberPart.replace(number, ""),
                multiplier = {
                    's': 1,
                    'm': 60,
                    'h': 3600,
                    'd': 86400
                };
            return (12 * number) * (multiplier[unit] || 1);
        },
        /**
         * Given a projection function (returned from createRegressionFunction) this method
         * creates all the "y" values from the given "x" values.
         * @access public
         * @param {Function}
         *            The function that returns the y given an x
         * @param {integer}
         *            Unix timestamp of the start of the viewable range of the current chart
         * @param {integer}
         *            Unix timestamp of the end of the viewable range of the current chart
         * @returns {Array}
         *            Array of x and y values
         **/
        createRegressionData: function(projectionFn, start, end) {
            var regression = [],
                downsample = this.request.downsample,
                config = this.config,
                i, y, skipThisPoint = false,
                step = this.__convertDownsampletoStep(downsample), t = start;
            while (t < end) {
                y = projectionFn(t);
                // make sure it is always visible in the graph (does not go below miny)
                if (config.miny !== undefined && config.miny != null && y <= config.miny) {
                    y = config.miny;
                    skipThisPoint = true;
                }

                // make sure it doesn't go above maxy
                if (config.maxy !== undefined && config.maxy != null && y >= config.maxy) {
                    y = config.maxy;
                    skipThisPoint = true;
                }

                if (!skipThisPoint) {
                    regression.push({
                        series: 0,
                        x: t * 1000, // nvd3 works in milliseconds
                        y: y
                    });
                }
                t = t + step;
                skipThisPoint = false;
            }
            return regression;
        },
        /**
         * Looks up and calls a projection algorithm with the projection config and values used in the projection
         * The projectionAlgorithm property is used to determine which function to call. It must exist on the
         * zenoss.visualization.projections namespace
         * @access public
         * @param {Object}
         *            Config settings for the projection (notably used in this method is "projectionAlgorithm")
         * @param {Array}
         *            Values returned from the metric service that are used to create the projection function
         * @returns {Function}
         *            Function that can be used to project y values given an x
         **/
        createRegressionFunction: function(projection, values) {
            // get the implementation based on the projection "type" (or projectionAlgorithm property)
            var xValues =  $.map(values, function(o) { return o["timestamp"]; }),
                yValues = $.map(values, function(o) { return o["value"]; });

            return zenoss.visualization.projections[projection.projectionAlgorithm](projection, xValues, yValues);
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

                if(config.range){
                    if(config.range.start){
                        request.start = config.range.start;
                    }

                    if(config.range.end){
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

                // if no downsample provided, calculate one
                // based on the range
                } else {
                    var start, end, delta;

                    // if no start time, assume 1hr-ago (default)
                    // NOTE - this uses local time which may not
                    // be the expected timezone
                    start = utils.createDate(request.start || "1h-ago");

                    // if no end time, assume now (default)
                    // NOTE - this uses local time which may not
                    // be the expected timezone
                    end = utils.createDate(request.end || "0s-ago");

                    delta = end.valueOf() - start.valueOf();

                    // iterate the DOWNSAMPLE list and choose the one which
                    // is closest to delta, defaulting to null if delta is too small
                    request.downsample = DOWNSAMPLE.reduce(function(acc, val){
                        return delta >= val[0] ? val[1] : acc;
                    }, null);
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
         * In the case of projections we need the past data to create the projection.
         * This creates the performance query for fetching that data
         * @access private
         * @param {object}
         *            config the config from which to build a request
         * @returns {object} a request object that can be POST-ed to the Zenoss
         *          performance metric service
         */
        __buildProjectionRequest: function(config, dataRequest, projection) {
            var request = {
                metrics: []
            }, start, end, delta, self = this;
            if (!projection.metric) {
                return false;
            }
            dataRequest.metrics.forEach(function(m){
                var metric, test = m.metric || m.name;
                // use the datapoint name for the case of rpn metrics
                if (test.indexOf(projection.metric.split("_")[1]) != -1) {
                    // copy of the object
                    metric = $.extend(true, {}, m);
                    metric.aggregator = projection.aggregateFunction || "max";
                    metric.emit = true;
                    if (self.plotInfo[metric.name || metric.metric]) {
                        projection.legend = "Projected " +self.plotInfo[metric.name || metric.metric].legend;
                    }
                    request.metrics.push(metric);
                }
            });
            if (!request.metrics.length) {
                return false;
            }

            request.returnset = config.returnset;
            request.tags = dataRequest.tags;
            request.end = parseInt(new Date().getTime()/1000); // now
            start = moment();
            start.subtract(projection.pastData[0], projection.pastData[1]);
            request.start = start.unix();

            if (config !== undefined) {

                request.series = true;

                // if no start time, assume 1hr-ago (default)
                // NOTE - this uses local time which may not
                // be the expected timezone
                start = utils.createDate(request.start || "1h-ago");

                // if no end time, assume now (default)
                // NOTE - this uses local time which may not
                // be the expected timezone
                end = utils.createDate(request.end || "0s-ago");

                delta = (end.valueOf() - start.valueOf()) * 1000;

                // iterate the DOWNSAMPLE list and choose the one which
                // is closest to delta, defaulting to null if delta is too small
                request.downsample = DOWNSAMPLE.reduce(function(acc, val){
                    return delta >= val[0] ? val[1] : acc;
                }, null);
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
         * value of all series datapoints in that response.
         * if nonZero is set to true, this will return the smallest non-zero
         * value
         */
        calculateResultsMin: function(data, nonZero){
            // if nonZero, set things up to start from Infinity
            var minStartValue = nonZero ?  Infinity : 0,
                result;

            result = data.reduce(function(acc, series){
                return Math.min(acc, series.datapoints.reduce(function(acc, dp){
                    // if the value is the string "NaN", ignore this dp
                    if(dp.value === "NaN") return acc;
                    if(nonZero && dp.value === 0) return acc;
                    return Math.min(acc, +dp.value);
                }, minStartValue));
            }, minStartValue);

            // if the result is Infinity, then all the values
            // were zero, so just return zero
            if(result === Infinity){
                result = 0;
            }

            return result;
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
            var val = this.calculateResultsMax(data),
                x, unitIndex;

            // if maxy is set, constrain the value based on that
            if(this.maxy !== undefined){
                val = this.maxy;
            }

            // if miny is set and val is less than miny, set val to miny
            if(this.miny !== undefined && val < this.miny){
                val = this.miny;
            }

            if(val === 0){
                unitIndex = 0;
            } else {
                x = Math.log(Math.abs(val)) / Math.log(this.base);
                unitIndex = Math.floor(x);
            }

            this.preferredYUnit = unitIndex;
        }
    };

    var SYMBOLS = {
        "-8": "y",
        "-7:": "z",
        "-6": "a",
        "-5": "f",
        "-4": "p",
        "-3": "n",
        "-2": "u",
        "-1": "m",
        "0": "",
        "1": "k",
        "2": "M",
        "3": "G",
        "4": "T",
        "5": "P",
        "6": "E",
        "7": "Z",
        "8": "Y"
    };

    function toEng(val, preferredUnit, format, base){
        var result,
            unit;

        // if preferredUnit is provided, target that value
        if(preferredUnit !== undefined){
            unit = preferredUnit;
        } else if(val === 0){
            unit = 0;
        } else {
            unit = Math.floor(Math.log(Math.abs(val)) / Math.log(base));
        }

        // TODO - if Math.abs(unit) > 8, return value in scientific notation
        result = val / Math.pow(base, unit);

        try{
            // if sprintf is passed a format it doesn't understand an exception is thrown
            return sprintf(format, result) + SYMBOLS[unit];
        } catch(err) {
            return sprintf(DEFAULT_NUMBER_FORMAT, result) + SYMBOLS[unit];
        }
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