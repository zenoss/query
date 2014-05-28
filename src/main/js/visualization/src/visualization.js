/**
 * visualization.js
 * create main visualization config object
 */
(function(){
    "use strict";

    var dateFormat = "MM/DD/YY hh:mm:ss a",
        showLegendOnNoData = true,
        url = "http://localhost:8080",
        urlPath = "/static/performance/query/",
        urlPerformance = "/api/performance/query/",
        DEFAULT_NUMBER_FORMAT = "%6.2f",
        __charts = {};

    function dateFormatter(date, timezone) {
        return moment.utc(date, "X").tz(timezone).format(dateFormat);
    }

    function tickFormat(start, end, ts, timezone) {
        var _start, _end, ts_seconds;

        /*
         * Convert the strings to date instances, with the understanding
         * that that data strings may be the one passed back from the
         * metric service that have '-' instead of spaces
         */
        if ($.isNumeric(start)) {
            _start = new Date(start * 1000);
        } else {
            _start = start;
        }

        if ($.isNumeric(end)) {
            _end = new Date(end * 1000);
        } else {
            _end = end;
        }

        //NOTE: Javascript timestamps are usually in milliseconds,
        // but moment.js uses seconds so we have to divide by 1000
        ts_seconds = ts / 1000;
        // Select a date/time format based on the range
        if (_start.getFullYear() === _end.getFullYear()) {
            if (_start.getMonth() === _end.getMonth()) {
                if (_start.getDate() === _end.getDate()) {
                    if (_start.getHours() === _end.getHours()) {
                        if (_start.getMinutes() === _end.getMinutes()) {
                            // only show seconds
                            return moment.utc(ts_seconds, "X").tz(timezone).format("::ss");
                        }
                        // show minutes and seconds
                        return moment.utc(ts_seconds, "X").tz(timezone).format(":mm :ss");
                    }
                    // hours, minutes and seconds
                    return moment.utc(ts_seconds, "X").tz(timezone).format("hh:mm:ssa");
                }
            }
            //show the date
            return moment.utc(ts_seconds, "X").tz(timezone).format("MM/DD-hh:mm:ssa");
        }
        // show the full date
        return moment.utc(ts_seconds, "X").tz(timezone).format(dateFormat);
    }

    function __cull(chart) {

        var i, keys = [];
        /*
         * If there is only one plot in the chart we are done, there is
         * nothing to be done.
         */
        if (chart.plots.length < 2) {
            return;
        }

        chart.plots.forEach(function(plot) {
            plot.values.forEach(function(v) {
                if (keys[v.x] === undefined) {
                    keys[v.x] = 1;
                } else {
                    keys[v.x] += 1;
                }
            });
        });

        // At this point, any entry in the keys array with a count of
        // chart.plots.length is a key in every plot and we can use, so
        // now
        // we walk through the plots again removing any invalid key
        chart.plots.forEach(function(plot) {
            for (i = plot.values.length - 1; i >= 0; i -= 1) {
                if (keys[plot.values[i].x] !== chart.plots.length) {
                    plot.values.splice(i, 1);
                }
            }
        });
    }

    function __reduceMax(group) {
        return group.reduce(function(p, v) {
            if (p.values[v.y] === undefined) {
                p.values[v.y] = 1;
            } else {
                p.values[v.y] += 1;
            }
            p.max = Math.max(p.max, v.y);
            return p;
        }, function(p, v) {
            var k;
            // need to remove the value from the values array
            p.values[v.y] -= 1;
            if (p.values[v.y] <= 0) {
                delete p.values[v.y];
                if (p.max === v.y) {
                    // pick new max, by iterating over keys
                    // finding the largest.
                    p.max = -1;
                    for (k in p.values) {
                        if (p.values.hasOwnProperty(k)) {
                            p.max = Math.max(p.max, parseFloat(k));
                        }
                    }
                }
            }
            p.total -= v.y;
            return p;
        }, function() {
            return {
                values : {},
                max : -1,
                toString : function() {
                    return this.max;
                }
            };
        });
    }

    function __showError(name, detail) {
        __showMessage(name, '<span class="zenerror">' + detail + '</span>');
    }

    function __showNoData(name) {
        __showMessage(name, '<span class="nodata"></span>');
    }

    function __hideMessage(name) {
        $('#' + name + ' .message').css('display', 'none');
    }

    function __showMessage(name, message) {
        if (message) {
            $('#' + name + ' .message').html(message);
        }
        __hideChart(name);

        // Center the message in the div
        $('#' + name + ' .message').css('display', 'block');
        $('#' + name + ' .message span').css('position', 'relative');
        $('#' + name + ' .message span').width(
            $('#' + name + ' .message').width() -
                parseInt($('#' + name + ' .message span')
                        .css('margin-left'), 10) -
                parseInt($('#' + name + ' .message span')
                        .css('margin-right'), 10));
        $('#' + name + ' .message span').css('top', '50%');
        $('#' + name + ' .message span')
            .css(
                'margin-top',
                -parseInt($('#' + name + ' .message span')
                    .height(), 10) / 2);
    }

    function __hideChart(name) {
        $('#' + name + ' .zenchart').css('display', 'none');

        if (!showLegendOnNoData) {
            $('#' + name + ' .zenfooter').css('display', 'none');
        }
    }

    function __showChart(name) {
        __hideMessage(name);
        $('#' + name + ' .zenchart').css('display', 'block');
        $('#' + name + ' .zenfooter').css('display', 'block');
    }

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
        url: url,

        /**
         * The url path where the static javascript dependencies can be
         * found. This includes library dependencies like jquery.
         *
         * @access public
         * @default /static/performance/query
         */
        urlPath: urlPath,

        /**
         * The url path where metrics are fetched from the server
         *
         * @access public
         * @default /api/performance/query
         */
        urlPerformance: urlPerformance,

        /**
         * Determines if the legend is displayed when no data is available
         * for any plot in the grid.
         *
         * @access public
         * @default true
         */
        showLegendOnNoData: showLegendOnNoData,

        /**
         * Used for formatting the date in the legend of the chart.
         * It must be a valid moment.js date format.
         * http://momentjs.com/docs/#/parsing/string-format/
         * @access public
         * @default "MM/DD/YY hh:mm:ss a"
         */
        dateFormat: dateFormat,

        DEFAULT_NUMBER_FORMAT: DEFAULT_NUMBER_FORMAT,

        /*
         * Symbols used during autoscaling
         */
        __scaleSymbols: [ 'y', // 10e-24 Yecto
            'z', // 10^-21 Zepto
            'a', // 10^-18 Atto
            'f', // 10^-15 Femto
            'p', // 10^-12 Pico
            'n', // 10^-9 Nano
            'u', // 10^-6 Micro
            'm', // 10^-3 Milli
            ' ', // Base
            'k', // 10^3 Kilo
            'M', // 10^6 Mega
            'G', // 10^9 Giga
            'T', // 10^12 Tera
            'P', // 10^15 Peta
            'E', // 10^18 Exa
            'Z', // 10^21 Zetta
            'Y' // 10^24 Yotta
        ],

        /**
         * Used to format dates for the output display in the footer of a
         * chart.
         *
         * @param {int}
         *            unix timestamp of the date to be formated
         * @returns a string representation of the date
         * @access public
         */
        dateFormatter: dateFormatter,

        /**
         * Used to generate the date/time to be displayed on a tick mark.
         * This takes into account the range of times being displayed so
         * that common data can be removed.
         *
         * @param {Date}
         *            start the start date of the time range being
         *            considered
         * @param {Date}
         *            end the end of the time range being considerd
         * @param {timestamp}
         *            ts the timestamp to be formated in ms since epoch
         * @returns string representation of the timestamp
         * @access public
         */
        tickFormat: tickFormat,

        /**
         * Culls the plots in a chart so that only data points with a common
         * time stamp remain.
         *
         * @param the
         *            chart that contains the plots to cull
         * @access private
         */
        __cull: __cull,

        __reduceMax: __reduceMax,

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
        __showError: __showError,

        /**
         * Shows a no data available message in the chart and hides any
         * chart elements such as the chart and the footer.
         *
         * @access private
         * @param {string}
         *            name of the div wrapper for the chart
         */
        __showNoData: __showNoData,

        /**
         * Hides the message window
         *
         * @access private
         * @param {string}
         *            name of the div wrapper for the chart
         */
        __hideMessage: __hideMessage,

        /**
         * Show the message window and hide the chart elements. The message
         * window is then populated with the given message.
         *
         * @access private
         * @param {string}
         *            name of the div wrapper for the chart
         * @param {string}
         *            html that represents the message to display.
         */
        __showMessage: __showMessage,

        /**
         * Hides the chart elements
         *
         * @access private
         * @param {string}
         *            name of the div wrapper of the chart
         */
        __hideChart: __hideChart,

        /**
         * Shows the chart elements
         *
         * @access private
         * @param {string}
         *            name of the div wrapper of the chart
         */
        __showChart: __showChart,

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
                var found = __charts[name];
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
            create : function(name, arg1, arg2, success, fail) {

                function loadChart(name, callback, onerror) {
                    var _callback = callback;
                    if (debug.debug) {
                        debug.__log('Loading chart from: ' +
                            url + '/chart/name/' +
                            name);
                    }
                    $.ajax({
                        'url' : url + '/chart/name/' + name,
                        'type' : 'GET',
                        'dataType' : 'json',
                        'contentType' : 'application/json',
                        'success' : function(data) {
                            _callback(data);
                        },
                        'error' : function(response) {
                            var err, detail;
                            debug.__error(response.responseText);
                            err = JSON.parse(response.responseText);
                            detail = 'Error while attempting to fetch chart resource with the name "' +
                                name + '", via the URL "' +
                                url + '/chart/name/' +
                                name + '", the reported error was "' +
                                err.errorSource + ':' + err.errorMessage + '"';
                            if (onerror !== undefined) {
                                onerror(err, detail);
                            }
                        }
                    });
                }

                var config, template, result;
                if (typeof arg1 === 'string') {
                    // A chart template name was specified, so we need to
                    // first
                    // load that template and then create the chart based on
                    // that.
                    config = arg2;
                    if (window.jQuery === undefined) {
                        dependency.__bootstrap(function() {
                            loadChart(arg1, function(template){
                                var merged = new Chart(name, utils.__merge(template, config));
                                __charts[name] = merged;
                                return merged;
                            }, function(err, detail) {
                                __showError(name, detail);
                            }
                            );
                        });
                        return;
                    }
                    loadChart(arg1, function(template) {
                        var merged = new Chart(name,
                            utils.__merge(template,
                                config));
                        __charts[name] = merged;
                        return merged;
                    }, function(err, detail) {
                        __showError(name, detail);
                    });
                    return;
                }

                template = null;
                config = arg1;

                if (window.jQuery === undefined) {
                    dependency.__bootstrap(function() {
                        var merged = new Chart(name, utils.__merge(template, config));
                        __charts[name] = merged;
                        return merged;
                    });
                    return;
                }
                result = new Chart(name, utils.__merge(template, config));
                __charts[name] = result;
            }
        },

        /**
         * Used to track the charts that have been created and the names to
         * which they are associated
         *
         * @access private
         */
        __charts : __charts,
    };

})();