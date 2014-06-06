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
         * @returns {object} the merged object
         */
        __merge: function __merge(base, extend) {
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
                    if (v.constructor === Number || v.constructor === String) {
                        m[k] = v;
                    } else if (v instanceof Array) {
                        m[k] = $.merge(m[k], v);
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