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
        relativeTimeToMS: relativeTimeToMS,

        shortId: function(targetLength){
            targetLength = targetLength || 10;
            var shortIdChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
                shortId = [];

            while(shortId.length < targetLength){
                shortId.push(shortIdChars[Math.floor(Math.random() * shortIdChars.length)]);
            }

            return shortId.join("");
        },

        compareASC: function(a, b, key) {
            return (a[key] < b[key]) ? -1 : (a[key] > b[key]) ? 1 : 0;
        }

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
