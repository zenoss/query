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

    // Chart method
    function dateFormatter(date, timezone) {
        return moment.utc(date, "X").tz(timezone).format(dateFormat);
    }

    // chart method
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

    // Chart method
    function __showError(name, detail) {
        __showMessage(name, '<span class="zenerror">' + detail + '</span>');
    }

    // Chart method
    function __showNoData(name) {
        __showMessage(name, '<span class="nodata"></span>');
    }


    // private methods
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
            create : function(name, config) {

                if (!window.jQuery) {
                    dependency.__bootstrap(function() {
                        __charts[name] = new Chart(name, config);
                    });
                    return;
                }

                __charts[name] = new Chart(name, config);
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