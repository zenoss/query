/**
 * @overview Provides objects and convenience methods to construct and
 *           manipulate Zenoss visualization graphs.
 * @copyright 2013, Zenoss, Inc; All rights reserved
 */
/**
 * @namespace
 */
(function(window) {
    "use strict";

    var DEFAULT_NUMBER_FORMAT = "%6.2f";

    /**
     * @namespace zenoss
     */
    var zenoss = {

        /**
         * @memberOf zenoss
         * @namespace
         * @access public
         */
        visualization : {

            /**
             * Used to enable (true) or disable (false) debug output to the
             * browser console
             *
             * @access public
             * @default false
             */
            debug : false,

            /**
             * Used to specify the base URL that is the endpoint for the Zenoss
             * metric service.
             *
             * @access public
             * @default http://localhost:8080
             */
            url : "http://localhost:8080",

            /**
             * The url path where the static javascript dependencies can be
             * found. This includes library dependencies like jquery.
             *
             * @access public
             * @default /static/performance/query
             */
            urlPath : "/static/performance/query/",

            /**
             * The url path where metrics are fetched from the server
             *
             * @access public
             * @default /api/performance/query
             */
            urlPerformance : "/api/performance/query/",
            /**
             * Used to format dates for the output display in the footer of a
             * chart.
             *
             * @param {Date}
             *            date the date to be formated
             * @returns a string representation of the date
             * @access public
             */
            dateFormatter : function(date) {
                return d3.time.format('%-m/%-d/%Y %-I:%M:%S %p')(date);
            },

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
            tickFormat : function(start, end, ts) {
                var _start, _end;

                /*
                 * Convert the strings to date instances, with the understanding
                 * that that data strings may be the one passed back from the
                 * metric service that have '-' instead of spaces
                 */
                if (typeof start === 'string') {
                    _start = new Date(start.replace(/-([^-])/g, ' $1'));
                } else {
                    _start = start;
                }

                if (typeof end === 'string') {
                    _end = new Date(end.replace(/-([^-])/g, ' $1'));
                } else {
                    _end = end;
                }

                // Select a date/time format based on the range
                if (_start.getFullYear() === _end.getFullYear()) {
                    if (_start.getMonth() === _end.getMonth()) {
                        if (_start.getDate() === _end.getDate()) {
                            if (_start.getHours() === _end.getHours()) {
                                if (_start.getMinutes() === _end.getMinutes()) {
                                    return d3.time.format('::%S')(new Date(ts));
                                }
                                return d3.time.format(':%M:%S')(new Date(ts));
                            }
                            return d3.time.format('%H:%M:%S')(new Date(ts));
                        }
                    }
                    return d3.time.format('%m/%d %H:%M:%S')(new Date(ts));
                }
                return d3.time.format('%x %X')(new Date(ts));
            },

            __group : function() {
                if (console !== undefined) {
                    if (console.group !== undefined) {
                        console.group.apply(console, arguments);
                    } else if (console.log !== undefined) {
                        console.log.apply(console, arguments);
                    }
                    // Oh well
                }
            },

            __groupCollapsed : function() {
                if (console !== undefined) {
                    if (console.groupCollapsed !== undefined) {
                        console.groupCollapsed.apply(console, arguments);
                    } else if (console.log !== undefined) {
                        console.log.apply(console, arguments);
                    }
                    // Oh well
                }
            },

            __groupEnd : function() {
                if (console !== undefined) {
                    if (console.groupEnd !== undefined) {
                        console.groupEnd.apply(console, arguments);
                    } else if (console.log !== undefined) {
                        console.log.apply(console, [ "END" ]);
                    }
                    // Oh well
                }
            },

            __error : function() {
                if (console !== undefined) {
                    if (console.error !== undefined) {
                        console.error.apply(console, arguments);
                    } else if (console.log !== undefined) {
                        console.log.apply(console, arguments);
                    }
                    // If neither of those exists, oh well ....
                }
            },

            __warn : function() {
                if (console !== undefined) {
                    if (console.warn !== undefined) {
                        console.warn.apply(console, arguments);
                    } else if (console.log !== undefined) {
                        console.log.apply(console, arguments);
                    }
                    // If neither of those exists, oh well ....
                }
            },

            __log : function() {
                if (console !== undefined) {
                    if (console.log !== undefined) {
                        console.log.apply(console, arguments);
                    }
                    // Oh well
                }
            },

            /**
             * Culls the plots in a chart so that only data points with a common
             * time stamp remain.
             *
             * @param the
             *            chart that contains the plots to cull
             * @access private
             */
            __cull : function(chart) {

                var i, keys = [];
                // If there is only one plot in the chart we are done, there is
                // nothing
                // to do.
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
            },

            __reduceMax : function(group) {
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
            __showError : function(name, detail) {
                zenoss.visualization.__showMessage(name,
                        '<span class="zenerror">' + detail + '</span>');
            },

            __showNoData : function(name) {
                zenoss.visualization.__showMessage(name,
                        '<span class="nodata"></span>');
            },

            __hideMessage : function(name) {
                $('#' + name + ' .message').css('display', 'none');
            },

            __showMessage : function(name, message) {
                if (message) {
                    $('#' + name + ' .message').html(message);
                }
                zenoss.visualization.__hideChart(name);

                // Center the message in the div
                $('#' + name + ' .message').css('display', 'block');
                $('#' + name + ' .message span').css('position', 'relative');
                $('#' + name + ' .message span').width(
                        $('#' + name + ' .message').width()
                                - parseInt($('#' + name + ' .message span')
                                        .css('margin-left'))
                                - parseInt($('#' + name + ' .message span')
                                        .css('margin-right')));
                $('#' + name + ' .message span').css('top', '50%');
                $('#' + name + ' .message span')
                        .css(
                                'margin-top',
                                -parseInt($('#' + name + ' .message span')
                                        .height()) / 2);
            },

            __hideChart : function(name) {
                $('#' + name + ' .zenchart').css('display', 'none');
                $('#' + name + ' .zenfooter').css('display', 'none');
            },

            __showChart : function(name) {
                zenoss.visualization.__hideMessage(name);
                $('#' + name + ' .zenchart').css('display', 'block');
                $('#' + name + ' .zenfooter').css('display', 'block');

            },

            Error : function(name, message) {
                this.name = name;
                this.message = message;
                this.toString = function() {
                    return this.name + ' : ' + this.message;
                };
            },

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
            Chart : function(name, config) {
                var self = this, dp, i;

                this.name = name;
                this.config = config;
                this.yAxisLabel = config.yAxisLabel;
                this.div = $('#' + this.name);
                if (this.div[0] === undefined) {
                    throw new zenoss.visualization.Error('SelectorError',
                            'unknown selector specified, "' + this.name + '"');
                }

                // Build up a map of metric name to legend label.
                this.legend = {};
                this.colors = {};

                for (i in this.config.datapoints) {
                    dp = this.config.datapoints[i];
                    this.legend[dp.metric] = dp.legend || dp.metric;
                    this.colors[dp.metric] = dp.color;
                }
                this.overlays = config.overlays || [];
                // set the format or a default
                this.format = config.format || zenoss.visualization.defaultNumberFormat;
                this.svgwrapper = document.createElement('div');
                $(this.svgwrapper).addClass('zenchart');
                $(this.div).append($(this.svgwrapper));
                this.containerSelector = '#' + name + ' .zenchart';

                this.message = document.createElement('div');
                $(this.message).addClass('message');
                $(this.message).css('display', 'none');
                $(this.div).append($(this.message));

                this.footer = document.createElement('div');
                $(this.footer).addClass('zenfooter');
                $(this.div).append($(this.footer));

                this.svg = d3.select(this.svgwrapper).append('svg');
                this.request = this.__buildDataRequest(this.config);

                if (zenoss.visualization.debug) {
                    zenoss.visualization
                            .__groupCollapsed('POST Request Object');
                    zenoss.visualization.__log(zenoss.visualization.url
                            + zenoss.visualization.urlPerformance);
                    zenoss.visualization.__log(this.request);
                    zenoss.visualization.__groupEnd();
                }

                // Sanity Check. If the request contained no metrics to query
                // then
                // log this information as a warning, as it really does not make
                // sense.
                if (this.request.metrics === undefined) {
                    zenoss.visualization
                            .__warn('Chart configuration contains no metric sepcifications. No data will be displayed.');
                } else {
                    $
                            .ajax({
                                'url' : zenoss.visualization.url
                                        + zenoss.visualization.urlPerformance,
                                'type' : 'POST',
                                'data' : JSON.stringify(this.request),
                                'dataType' : 'json',
                                'contentType' : 'application/json',
                                'success' : function(data) {
                                    self.plots = self.__processResult(
                                            self.request, data);
                                    // Set default type of the chart if it was
                                    // not
                                    // set
                                    if (self.config.type === undefined) {
                                        self.config.type = 'line';
                                    }
                                    self.__render(data);
                                },
                                'error' : function(res) {
                                    var detail;
                                    // Many, many reasons that we might have
                                    // gotten
                                    // here, with most of them we are not able
                                    // to
                                    // detect why.
                                    // If we have a readystate of 4 and an
                                    // response
                                    // code in the
                                    // 200s that likely means we were unable to
                                    // parse the JSON
                                    // returned from the server. If not that
                                    // then
                                    // who knows
                                    // ....
                                    if (res.readyState === 4
                                            && Math.floor(res.status / 100) === 2) {
                                        detail = 'Severe: Unable to parse data returned from Zenoss metric service as JSON object. Please copy / paste the REQUEST and RESPONSE written to your browser\'s Java Console into an email to Zenoss Support';
                                        zenoss.visualization
                                                .__group('Severe error, please report');
                                        zenoss.visualization
                                                .__error(
                                                        'REQUEST : POST '
                                                                + zenoss.visualization.urlPerformance
                                                                + ' ',
                                                        +JSON
                                                                .stringify(self.request));
                                        zenoss.visualization
                                                .__error('RESPONSE: '
                                                        + res.responseText);
                                        zenoss.visualization.__groupEnd();
                                        zenoss.visualization.__showError(
                                                self.name, detail);
                                    } else {
                                        try {
                                            var err = JSON
                                                    .parse(res.responseText);
                                            detail = 'An unexpected failure response was received from the server. The reported message is: '
                                                    + err.errorSource
                                                    + ' : '
                                                    + err.errorMessage;
                                            zenoss.visualization
                                                    .__error(detail);
                                            zenoss.visualization.__showError(
                                                    self.name, detail);
                                        } catch (e) {
                                            detail = 'An unexpected failure response was received from the server. The reported message is: '
                                                    + res.statusText
                                                    + ' : '
                                                    + res.status;
                                            zenoss.visualization
                                                    .__error(detail);
                                            zenoss.visualization.__showError(
                                                    self.name, detail);
                                        }
                                    }
                                }
                            });
                }
            },

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
                    var found = zenoss.visualization.__charts[name];
                    if (found === undefined) {
                        zenoss.visualization
                                .__warn('Attempt to modify a chart, "' + name
                                        + '", that does not exist.');
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
                        if (zenoss.visualization.debug) {
                            zenoss.visualization.__log('Loading chart from: '
                                    + zenoss.visualization.url + '/chart/name/'
                                    + name);
                        }
                        $
                                .ajax({
                                    'url' : zenoss.visualization.url
                                            + '/chart/name/' + name,
                                    'type' : 'GET',
                                    'dataType' : 'json',
                                    'contentType' : 'application/json',
                                    'success' : function(data) {
                                        _callback(data);
                                    },
                                    'error' : function(response) {
                                        zenoss.visualization
                                                .__error(response.responseText);
                                        var err = JSON
                                                .parse(response.responseText);
                                        var detail = 'Error while attempting to fetch chart resource with the name "'
                                                + name
                                                + '", via the URL "'
                                                + zenoss.visualization.url
                                                + '/chart/name/'
                                                + name
                                                + '", the reported error was "'
                                                + err.errorSource
                                                + ':'
                                                + err.errorMessage + '"';
                                        if (onerror !== undefined) {
                                            onerror(err, detail);
                                        }
                                    }
                                });
                    }

                    var config, template;
                    if (typeof arg1 === 'string') {
                        // A chart template name was specified, so we need to
                        // first
                        // load that template and then create the chart based on
                        // that.
                        config = arg2;
                        if (window.jQuery === undefined) {
                            zenoss.visualization
                                    .__bootstrap(function() {
                                        loadChart(
                                                arg1,
                                                function(template) {
                                                    var result = new zenoss.visualization.Chart(
                                                            name,
                                                            zenoss.visualization
                                                                    .__merge(
                                                                            template,
                                                                            config));
                                                    zenoss.visualization.__charts[name] = result;
                                                    return result;
                                                }, function(err, detail) {
                                                    zenoss.visualization
                                                            .__showError(name,
                                                                    detail);
                                                });
                                    });
                            return;
                        }
                        loadChart(arg1, function(template) {
                            var result = new zenoss.visualization.Chart(name,
                                    zenoss.visualization.__merge(template,
                                            config));
                            zenoss.visualization.__charts[name] = result;
                            return result;
                        }, function(err, detail) {
                            zenoss.visualization.__showError(name, detail);
                        });
                        return;
                    }

                    template = null;
                    config = arg1;

                    if (window.jQuery === undefined) {
                        zenoss.visualization.__bootstrap(function() {
                            var result = new zenoss.visualization.Chart(name,
                                    zenoss.visualization.__merge(template,
                                            config));
                            zenoss.visualization.__charts[name] = result;
                            return result;
                        });
                        return;
                    }
                    var result = new zenoss.visualization.Chart(name,
                            zenoss.visualization.__merge(template, config));
                    zenoss.visualization.__charts[name] = result;
                    return result;
                }
            },

            /**
             * Used to track dependency loading, including the load state
             * (loaded / loading) as well as the callback that will be called
             * when a dependency load has been completed.
             *
             * @access private
             */
            __dependencies : {},

            /**
             * Used to track the charts that have been created and the names to
             * which they are associated
             *
             * @access private
             */
            __charts : {},

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
            load : function(callback) {
                zenoss.visualization.__bootstrap(callback);
            }
        }
    };

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

    /**
     * Sets the box in the footer for the given plot (specified by index) to the
     * specified color. The implementation of this is dependent on how the
     * footer is constructed (see __buildFooter).
     *
     * @access private
     * @param {int}
     *            idx the index of the plot whose color should be set,
     *            corresponds to which row in the table + 2
     * @param {color}
     *            the color to which the box should be set.
     */
    zenoss.visualization.Chart.prototype.__setFooterBoxColor = function(idx,
            color) {
        var box = $($(this.table).find('.zenfooter_box')[idx]);
        box.css('background-color', color.color);
        box.css('opacity', color.opacity);
    };

    /**
     * @access private
     * @param {number}
     *             The number we are formatting
     * @param {string}
     *             The format string for example "%2f";
     **/
    zenoss.visualization.Chart.prototype.formatValue = function(value) {
        var format = this.format;
        try{
            var rval =  parseFloat(sprintf(format, value));
            if ($.isNumeric(rval)) {
                return rval;
            }
            // if the result is a NaN just return the original value
            return value;
        } catch (x) {
            // override the number format for this chart
            // since this method could be called several times to render a chart.
            this.format = DEFAULT_NUMBER_FORMAT;
            zenoss.visualization.__warn('Invalid format string  ' + format + ' using the default format.');
            return parseFloat(sprintf(this.format, value));
        }
    };
    /**
     * Checks to see if the passed in plot is actually an overlay.
     * @access private
     * @param {object}
     *        plot the object representing the plot
     * @return boolean if the plot is an overlay
     **/
    zenoss.visualization.Chart.prototype.__isOverlay = function(plot) {
        var i;
        if (this.overlays.length) {
            for (i=0; i<this.overlays.length; i+=1) {
                if (this.overlays[i].legend === plot.key) {
                    return true;
                }
            }
        }
        return false;
    };

    zenoss.visualization.Chart.prototype.__updateFooter = function(data) {
        var plot, vals, cur, min, max, avg, cols, init, label, i, v, ll, k, rows;

        // The first table row is for the dates, the second is a header and then
        // a row for each plot.
        rows = $(this.table).find('tr');

        $($(rows[0]).find('td')).html(
                zenoss.visualization.dateFormatter(new Date(
                        data.startTimeActual.replace(/-([^-])/g, ' $1')))
                        + ' to '
                        + zenoss.visualization.dateFormatter(new Date(
                                data.endTimeActual.replace(/-([^-])/g, ' $1')))
                        + ' (' + jstz.determine().name() + ')');

        // Calculate the summary values from the data and place the date in the
        // the table.
        ll = this.plots.length;
        for (i = 0; i < ll; i += 1) {
            plot = this.plots[i];
            // do not add a footer box for overlays
            if (!this.__isOverlay(plot)) {
                vals = [ 0, -1, -1, 0 ];
                cur = 0;
                min = 1;
                max = 2;
                avg = 3;
                init = false;
                plot.values.forEach(function(v) {
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
                });
                vals[avg] = vals[avg] / plot.values.length;

                // The first column is the color, the second is the metric name,
                // followed byt the values
                cols = $(rows[2 + i]).find('td');

                // Metric name
                label = plot.key;
                if ((k = label.indexOf('{')) > -1) {
                    label = label.substring(0, k) + '{*}';
                }
                $(cols[1]).html(label);

                for (v = 0; v < vals.length; v += 1) {
                    $(cols[2 + v]).html(this.formatValue(vals[v]));
                }
            }
        }
    };

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
    zenoss.visualization.Chart.prototype.__buildFooter = function(config, data) {
        this.table = document.createElement('table');
        $(this.table).addClass('zenfooter_content');
        $(this.table).addClass('zenfooter_text');
        $(this.footer).append($(this.table));

        // One row for the date range of the chart
        var tr = document.createElement('tr');
        var td = document.createElement('td');
        var dates = document.createElement('span');
        $(td).addClass('zenfooter_dates');
        $(td).attr('colspan', 6);
        $(dates).addClass('zenfooter_dates_text');
        $(this.table).append($(tr));
        $(tr).append($(td));
        $(td).append($(dates));

        if (typeof config.footer === 'string' && config.footer === 'range') {
            return;
        }

        // One row for the stats table header
        tr = document.createElement('tr');
        var th;
        [ '', 'Metric', 'Ending', 'Minimum', 'Maximum', 'Average' ]
                .forEach(function(s) {
                    th = document.createElement('th');
                    $(th).addClass('footer_header');
                    $(th).html(s);
                    if (s.length === 0) {
                        $(th).addClass('zenfooter_box_column');
                    }
                    $(tr).append($(th));
                });
        $(this.table).append($(tr));

        // One row for each of the metrics
        var self = this;
        var d, i;
        for (i in this.plots) {

            if (!self.__isOverlay(this.plots[i])) {

                tr = document.createElement('tr');

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
                [ 1, 2, 3, 4 ].forEach(function() {
                    td = document.createElement('td');
                    $(td).addClass('zenfooter_data');
                    $(td).addClass('zenfooter_data_number');
                    $(tr).append($(td));
                });

                $(self.table).append($(tr));
            }
        }
        // Fill in the stats table
        this.__updateFooter(data);
    };

    /**
     * Updates a graph with the changes specified in the given change set. To
     * remove a value from the configuration its value should be set to a
     * negative sign, '-'.
     *
     * @param {object}
     *            changeset updates to the existing graph's configuration.
     */
    zenoss.visualization.Chart.prototype.update = function(changeset) {

        // This function is really meant to only handle given types of changes,
        // i.e. we don't expect that you can change the type of the graph but
        // you
        // should be able to change the date range.
        var self = this;
        this.config = zenoss.visualization.__merge(this.config, changeset);

        // A special check for the removal of items from the config. If the
        // value
        // for any item in the change set is '-', then we delete that key.
        var kill = [];
        var property;
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

        this.request = this.__buildDataRequest(this.config);
        $
                .ajax({
                    'url' : zenoss.visualization.url
                            + zenoss.visualization.urlPerformance,
                    'type' : 'POST',
                    'data' : JSON.stringify(this.request),
                    'dataType' : 'json',
                    'contentType' : 'application/json',
                    'success' : function(data) {
                        self.plots = self.__processResult(self.request, data);
                        // Set default type of the chart if it was not
                        // set
                        if (self.config.type === undefined) {
                            self.config.type = 'line';
                        }
                        self.__updateData(data);
                    },
                    'error' : function(res) {
                        // Many, many reasons that we might have gotten
                        // here, with most of them we are not able to
                        // detect why.
                        // If we have a readystate of 4 and an response
                        // code in the
                        // 200s that likely means we were unable to
                        // parse the JSON
                        // returned from the server. If not that then
                        // who knows
                        // ....
                        var detail;
                        if (res.readyState === 4
                                && Math.floor(res.status / 100) === 2) {
                            detail = 'Severe: Unable to parse data returned from Zenoss metric service as JSON object. Please copy / paste the REQUEST and RESPONSE written to your browser\'s Java Console into an email to Zenoss Support';
                            zenoss.visualization
                                    .__group('Severe error, please report');
                            zenoss.visualization.__error('REQUEST : POST '
                                    + zenoss.visualization.urlPerformance
                                    + '  ' + JSON.stringify(self.request));
                            zenoss.visualization.__error('RESPONSE: '
                                    + res.responseText);
                            zenoss.visualization.__groupEnd();
                            zenoss.visualization.__showError(self.name, detail);
                        } else {
                            try {
                                var err = JSON.parse(res.responseText);
                                detail = 'An unexpected failure response was received from the server. The reported message is: '
                                        + err.errorSource
                                        + ' : '
                                        + err.errorMessage;
                                zenoss.visualization.__error(detail);
                                zenoss.visualization.__showError(self.name,
                                        detail);
                            } catch (e) {
                                detail = 'An unexpected failure response was received from the server. The reported message is: '
                                        + res.statusText + ' : ' + res.status;
                                zenoss.visualization.__error(detail);
                                zenoss.visualization.__showError(self.name,
                                        detail);
                            }
                        }
                    }
                });
    };

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
    zenoss.visualization.Chart.prototype.__buildDataRequest = function(config) {
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

            if (config.series !== undefined) {
                request.series = config.series;
            }

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
                config.datapoints.forEach(function(dp) {
                    var m = {};
                    m.metric = dp.metric;
                    if (dp.rate !== undefined) {
                        m.rate = dp.rate;
                    }
                    if (dp.aggregator !== undefined) {
                        m.aggregator = dp.aggregator;
                    }
                    if (dp.downsample !== undefined) {
                        m.downsample = dp.downsample;
                    }
                    if (dp.expression !== undefined) {
                        m.expression = dp.expression;
                    }
                    if (dp.tags !== undefined) {
                        m.tags = {};
                        var key;
                        for (key in dp.tags) {
                            if (dp.tags.hasOwnProperty(key)) {
                                m.tags[key] = dp.tags[key];
                            }
                        }
                    }
                    request.metrics.push(m);
                });

            }
        }
        return request;
    };

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
    zenoss.visualization.Chart.prototype.__processResultAsSeries = function(
            request, data) {
        var self = this, plots = [];

        data.results.forEach(function(result) {
            // The key for a series plot will be its distinguishing
            // characteristics, which is the metric name and the
            // tags. We will use any mapping from metric name to legend value
            // that was part of the original request.
            var key = self.legend[result.metric];
            if (result.tags !== undefined) {
                key += '{';
                var prefix = '';
                var tag;
                for (tag in result.tags) {
                    if (result.tags.hasOwnProperty(tag)) {
                        key += prefix + tag + '=' + result.tags[tag];
                        prefix = ',';
                    }
                }
                key += '};';
            }
            var plot = {
                'key' : key,
                'color': self.colors[result.metric],
                'values' : []
            };
            result.datapoints.forEach(function(dp) {
                plot.values.push({
                    'x' : dp.timestamp * 1000,
                    'y' : dp.value
                });
            });
            plots.push(plot);
        });

        return plots;
    };

    /**
     * Processes the result from the Zenoss performance metric query that is in
     * the default format into the data that can be utilized by the chart
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
    zenoss.visualization.Chart.prototype.__processResultAsDefault = function(
            request, data) {

        var self = this, plotMap = [];

        // Create a plot for each metric name, this is essentially
        // grouping the results by metric name. This can cause problems
        // if the request contains multiple queries for the same
        // metric, but this is basically a restriction of the
        // implementation (OpenTSDB) where it doesn't split the results
        // logically when multiple requests are made in a single call.
        data.results.forEach(function(result) {
            var plot = plotMap[result.metric];
            if (plot === undefined) {
                plot = {
                    'key' : self.legend[result.metric],
                    'color': self.colors[result.metric],
                    'values' : []
                };
                plotMap[result.metric] = plot;
            }

            plot.values.push({
                'x' : result.timestamp * 1000,
                'y' : result.value
            });
        });

        var xcompare = function(a, b) {
            if (a.x < b.x) {
                return -1;
            }
            if (a.x > b.x) {
                return 1;
            }
            return 0;
        };

        // Convert the plotMap into an array of plots for the graph
        // library to process
        var plots = [];
        var key;
        for (key in plotMap) {
            if (plotMap.hasOwnProperty(key)) {
                // Sort the values of the plot as we put them in the
                // plots aray.
                plotMap[key].values.sort(xcompare);
                plots.push(plotMap[key]);
            }
        }
        return plots;
    };

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
    zenoss.visualization.Chart.prototype.__processResult = function(request,
            data) {
        var self = this, plots, i, overlay;


        if (data.series) {
             plots = this.__processResultAsSeries(request, data);
        }
        plots = this.__processResultAsDefault(request, data);

        // add overlays
        if (this.overlays.length && plots.length) {
            for (i in this.overlays) {
                overlay = this.overlays[i];
                // get the date range
                var minDate, maxDate, plot, i, firstMetric = plots[0];
                plot = {
                    'key' : overlay.legend,
                    'disabled': true,
                    'values' : [],
                    'color': overlay.color
                };
                minDate = firstMetric.values[0].x;
                maxDate = firstMetric.values[firstMetric.values.length - 1].x;
                for (i=0; i<overlay.values.length;i+=1) {

                    // create a line by putting a point at the start and a point at the end
                    plot.values.push({
                        x: minDate,
                        y: overlay.values[i]
                    });
                    plot.values.push({
                        x: maxDate,
                        y: overlay.values[i]
                    });
                }
                plots.push(plot);
            }
        }
        return plots;
    };

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
    zenoss.visualization.__merge = function(base, extend) {
        var m;
        if (zenoss.visualization.debug) {
            zenoss.visualization.__groupCollapsed('Object Merge');
            zenoss.visualization.__group('SOURCES');
            zenoss.visualization.__log(base);
            zenoss.visualization.__log(extend);
            zenoss.visualization.__groupEnd();
        }

        if (base === undefined || base === null) {
            m = $.extend(true, {}, extend);
            if (zenoss.visualization.debug) {
                zenoss.visualization.__log(m);
                zenoss.visualization.__groupEnd();
            }
            return m;
        }
        if (extend === undefined || extend === null) {
            m = $.extend(true, {}, base);
            if (zenoss.visualization.debug) {
                zenoss.visualization.__log(m);
                zenoss.visualization.__groupEnd();
            }
            return m;
        }

        m = $.extend(true, {}, base);
        var k, v;
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
                        m[k] = zenoss.visualization.__merge(m[k], v);
                    }
                } else {
                    m[k] = $.extend(m[k], v);
                }
            }
        }

        if (zenoss.visualization.debug) {
            zenoss.visualization.__log(m);
            zenoss.visualization.__groupEnd();
        }
        return m;
    };

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
    zenoss.visualization.__loadDependencies = function(required, callback) {
        var base;
        if (required === undefined) {
            callback();
            return;
        }

        // Check if it is already loaded, using the value in the 'defined' field
        var o;
        if (zenoss.visualization.__dependencies[required.defined] !== undefined
                && zenoss.visualization.__dependencies[required.defined].state !== undefined) {
            o = zenoss.visualization.__dependencies[required.defined].state;
        }
        if (o !== undefined) {
            if (o === 'loaded') {
                if (zenoss.visualization.debug) {
                    zenoss.visualization.__log('Dependencies for "'
                            + required.defined
                            + '" already loaded, continuing.');
                }
                // Already loaded, so just invoke the callback
                callback();
            } else {
                // It is in the process of being loaded, so add our callback to
                // the
                // list of callbacks to call when it is loaded.
                if (zenoss.visualization.debug) {
                    zenoss.visualization
                            .__log('Dependencies for "'
                                    + required.defined
                                    + '" in process of being loaded, queuing until loaded.');
                }

                var c = zenoss.visualization.__dependencies[required.defined].callbacks;
                c.push(callback);
            }
            return;
        } else {
            // OK, not yet loaded or being loaded, so it is ours.
            if (zenoss.visualization.debug) {
                zenoss.visualization
                        .__log('Dependencies for "'
                                + required.defined
                                + '" not loaded nor in process of loading, initiate loading.');
            }

            zenoss.visualization.__dependencies[required.defined] = {};
            base = zenoss.visualization.__dependencies[required.defined];
            base.state = 'loading';
            base.callbacks = [];
            base.callbacks.push(callback);
        }

        // Load the JS and CSS files. Divide the list of files into two lists:
        // JS
        // and CSS as we can load one async, and the other loads sync (CSS).
        var js = [];
        var css = [];
        required.source.forEach(function(v) {
            if (v.endsWith('.js')) {
                js.push(v);
            } else if (v.endsWith('.css')) {
                css.push(v);
            } else {
                zenoss.visualization.__warn('Unknown required file type, "' + v
                        + '" when loading dependencies for "' + 'unknown'
                        + '". Ignored.');
            }
        });

        base = zenoss.visualization.__dependencies[required.defined];
        zenoss.visualization.__load(js, css, function() {
            base.state = 'loaded';
            base.callbacks.forEach(function(c) {
                c();
            });
            base.callbacks.length = 0;
        });
    };

    zenoss.visualization.Chart.prototype.__updateData = function(data) {
        if (this.plots.length === 0) {
            zenoss.visualization.__showNoData(this.name);
        } else {
            zenoss.visualization.__showChart(this.name);
            this.impl.update(this, data);
            this.__updateFooter(data);
        }
    };

    /**
     * Loads the chart renderer as a dependency and then constructs and renders
     * the chart.
     *
     * @access private
     * @param {object}
     *            data the data that is being rendered in the graph
     */
    zenoss.visualization.Chart.prototype.__render = function(data) {
        var self = this;
        zenoss.visualization
                .__loadDependencies(
                        {
                            'defined' : self.config.type.replace('.', '_'),
                            'source' : [ 'charts/'
                                    + self.config.type.replace('.', '/')
                                    + '.js' ]
                        },
                        function() {
                            var i;
                            if (self.config.footer === undefined
                                    || (typeof self.config.footer === 'boolean' && self.config.footer === true)
                                    || (typeof self.config.footer === 'string' && self.config.footer === 'range')) {
                                self.__buildFooter(self.config, data);
                            }

                            try {
                                i = zenoss.visualization.chart;
                                self.config.type.split('.').forEach(
                                        function(seg) {
                                            i = i[seg];
                                        });
                                self.impl = i;
                            } catch (err) {
                                throw new zenoss.visualization.Error(
                                        'DependencyError',
                                        'Unable to locate loaded chart type, "'
                                                + self.config.type
                                                + '", error: ' + err);
                            }

                            // Check the impl to see if a dependency is listed
                            // and
                            // if so load that.
                            zenoss.visualization
                                    .__loadDependencies(
                                            self.impl.required,
                                            function() {
                                                $(self.svgwrapper)
                                                        .outerHeight(
                                                                $(self.div)
                                                                        .height()
                                                                        - $(
                                                                                self.footer)
                                                                                .outerHeight());
                                                self.closure = self.impl.build(
                                                        self, data);
                                                var _closure = self.closure;
                                                self.impl.render(self);

                                                // Set the colors in the footer
                                                // based on the
                                                // chart that was created.
                                                if (self.config.footer === undefined
                                                        || (typeof self.config.footer === 'boolean' && self.config.footer === true)) {
                                                    for (i = 0; i < self.plots.length; i += 1) {
                                                        var color = {
                                                            color: self.plots[i].color
                                                        };
                                                        if (!color.color) {
                                                            color = self.impl.color(self, _closure, i);
                                                        }
                                                        self.__setFooterBoxColor(i, color);
                                                    }
                                                }
                                            });
                        });
    };

    /**
     * Loads the CSS specified by the URL.
     *
     * @access private
     * @param {url}
     *            url the url, in string format, of the CSS file to load.
     */
    zenoss.visualization.__loadCSS = function(url) {
        var css = document.createElement('link');
        css.rel = 'stylesheet';
        css.type = 'text/css';

        if (!url.startsWith("http")) {
            css.href = zenoss.visualization.url + zenoss.visualization.urlPath
                    + url;
        } else {
            css.href = url;
        }
        document.getElementsByTagName('head')[0].appendChild(css);
    };

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
    zenoss.visualization.__bootstrapScriptLoader = function(url, callback) {

        function ZenDeferred() {
            var failCallback;

            this.fail = function(_) {
                if (!arguments.length) {
                    return failCallback;
                }
                failCallback = _;
            };
        }

        var script = document.createElement("script");
        var deferred = new ZenDeferred();
        var _callback = callback;
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
    };

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
    zenoss.visualization.__load = function(js, css, success, fail) {
        if (zenoss.visualization.debug) {
            zenoss.visualization.__log('Request to load "' + js + '" and "'
                    + css + '".');
        }
        if (js.length === 0) {
            // All JavaScript files are loaded, now the loading of CSS can begin
            css.forEach(function(uri) {
                zenoss.visualization.__loadCSS(uri);
                if (zenoss.visualization.debug) {
                    zenoss.visualization.__log('Loaded dependency "' + uri
                            + '".');
                }
            });
            if (typeof success === 'function') {
                success();
            }
            return;
        }

        // Shift the next value off of the JS array and load it.
        var uri = js.shift();
        var _js = js;
        var _css = css;
        var self = this;
        if (!uri.startsWith("http")) {
            uri = zenoss.visualization.url + zenoss.visualization.urlPath + uri;
        }

        var loader = zenoss.visualization.__bootstrapScriptLoader;
        if (window.jQuery !== undefined) {
            loader = $.getScript;
        }
        loader(uri, function() {
            if (zenoss.visualization.debug) {
                zenoss.visualization.__log('Loaded dependency "' + uri + '".');
            }
            self.__load(_js, _css, success, fail);
        })
                .fail(
                        function(_1, _2, exception) {
                            zenoss.visualization
                                    .__error('Unable to load dependency "'
                                            + uri
                                            + '", with error "'
                                            + exception
                                            + '". Loading halted, will continue, but additional errors likely.');
                            self.__load(_js, _css, success, fail);
                        });
    };

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
    zenoss.visualization.__bootstrap = function(success, fail) {
        zenoss.visualization.__loadDependencies({
            'defined' : 'd3',
            'source' : [ 'jquery.min.js', 'd3.v3.min.js', 'jstz-1.0.4.min.js',
                    'css/zenoss.css', 'sprintf.min.js' ]
        }, success, fail);
    };
    window.zenoss = zenoss;
}(window));
