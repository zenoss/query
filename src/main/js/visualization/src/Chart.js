/**
 * Chart.js
 * main chart object
 */
(function(){
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
    Chart = function(name, config) {
        this.name = name;
        this.config = config;
        this.yAxisLabel = config.yAxisLabel;
        this.div = $('#' + this.name);
        if (this.div[0] === undefined) {
            throw new utils.Error('SelectorError',
                    'unknown selector specified, "' + this.name + '"');
        }

        // Build up a map of metric name to legend label.
        this.__buildPlotInfo();

        this.overlays = config.overlays || [];
        // set the format or a default
        this.format = config.format || visualization.DEFAULT_NUMBER_FORMAT;
        if ($.isNumeric(config.miny)) {
            this.miny = config.miny;
        }
        if ($.isNumeric(config.maxy)) {
            this.maxy = config.maxy;
        }
        this.timezone = config.timezone || jstz.determine().name();
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
            visualization.__showError(this.name, x);
        }
    };

    /**
     * Returns the appropriate scale symbol given a scaling factor
     *
     * @access private
     * @param {number}
     *            scale factor, which is the the value which is multiplied by
     *            the scale unit and then applied to a value to get the
     *            displayed value.
     * @returns character the symbol associated widh the given scale factor
     */
    Chart.prototype = {
        constructor: Chart,

        __scaleSymbol: function(factor) {
            var ll, idx;
            ll = visualization.__scaleSymbols.length;
            idx = factor + ((ll - 1) / 2);
            if (idx < 0 || idx >= ll) {
                return 'UKN';
            }
            return visualization.__scaleSymbols[idx];
        },

        /**
         * Calculates a scale factor given the maximum value in the chart.
         *
         * @access private
         * @param {number}
         *            maximum value in the chart data
         * @returns number the calculated scale factor
         */
        __calculateAutoScaleFactor: function(max) {
            var factor = 0, ceiling, upper, lower, unit;
            if (this.config.autoscale) {
                ceiling = this.config.autoscale.ceiling || 5;
                unit = parseInt(this.config.autoscale.factor || 1000, 10);

                upper = Math.pow(10, ceiling);
                lower = upper / 10;

                // Make sure that max value is greater than the lower boundary
                while (max !== 0 && max < lower) {
                    max *= unit;
                    factor -= 1;
                }

                /*
                 * And then make sure that max is lower than the upper boundary, it
                 * is favored that number be less than the upper boundary than
                 * higher than the lower.
                 */
                while (max !== 0 && max > upper) {
                    max /= unit;
                    factor += 1;
                }
            }
            return factor;
        },

        /**
         * Set the auto scale information on the chart
         *
         * @access private
         * @param {number}
         *            auto scaling factor
         */
        __configAutoScale: function(factor) {
            var scaleUnit = 1000;
            if (this.config.autoscale && this.config.autoscale.factor) {
                scaleUnit = this.config.autoscale.factor;
            }
            this.scale = {};
            this.scale.factor = factor;
            this.scale.symbol = this.__scaleSymbol(factor);
            this.scale.term = Math.pow(scaleUnit, factor);
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
            var format = this.format, scaled, rval;

            /*
             * If we were given a undefined value, Infinity, of NaN (all things that
             * can't be formatted, then just return the value.
             */
            if (!$.isNumeric(value)) {
                return value;
            }
            try {
                scaled = value / this.scale.term;
                rval = sprintf(format, scaled);
                if ($.isNumeric(rval)) {
                    return rval + this.scale.symbol;
                }
                // if the result is a NaN just return the original value
                return rval;
            } catch (x) {
                // override the number format for this chart
                // since this method could be called several times to render a
                // chart.
                this.format = DEFAULT_NUMBER_FORMAT;
                debug.__warn('Invalid format string  ' + format +
                    ' using the default format.');
                scaled = value / this.scale.term;
                try {
                    return sprintf(this.format, scaled) + this.scale.symbol;
                } catch (x1) {
                    return scaled + this.scale.symbol;
                }
            }
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
            height = parseInt($(this.div).height(), 10) - fheight;
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
                sta = visualization.dateFormatter(data.startTimeActual, timezone );
                eta = visualization.dateFormatter(data.endTimeActual, timezone);
            } else {
                sta = eta = 'N/A';

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
                            color = 'white'; // unable to determine color
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

                        if (!plot) {
                            for (v = 2; v < 6; v += 1) {
                                $(cols[v]).html('N/A');
                            }
                        } else {
                            vals = [ 0, -1, -1, 0 ];
                            cur = 0;
                            min = 1;
                            max = 2;
                            avg = 3;
                            init = false;
                            for (vIdx in plot.values) {
                                v = plot.values[vIdx];
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
            this.config = utils.__merge(this.config, changeset);

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
                        var results = self.__processResult(self.request,
                                data);
                        self.plots = results[0];
                        self.__configAutoScale(results[1]);

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
                    },
                    'error' : function(res) {
                        /*
                         * Many, many reasons that we might have gotten
                         * here, with most of them we are not able to detect
                         * why. If we have a readystate of 4 and an response
                         * code in the 200s that likely means we were unable
                         * to parse the JSON returned from the server. If
                         * not that then who knows ....
                         */
                        self.plots = undefined;
                        if (self.__updateFooter()) {
                            self.__resize();
                        }

                        var err, detail;
                        if (res.readyState === 4 &&
                                Math.floor(res.status / 100) === 2) {
                            detail = 'Severe: Unable to parse data returned from Zenoss metric service as JSON object. Please copy / paste the REQUEST and RESPONSE written to your browser\'s Java Console into an email to Zenoss Support';
                            debug.__group('Severe error, please report');
                            debug.__error('REQUEST : POST ' + visualization.urlPerformance + '  ' + JSON.stringify(self.request));
                            debug.__error('RESPONSE: ' + res.responseText);
                            debug.__groupEnd();
                            visualization.__showError(self.name, detail);
                        } else {
                            try {
                                err = JSON.parse(res.responseText);
                                if (!err || !err.errorSoruce || !err.errorMessage) {
                                    detail = 'An unexpected failure response was received from the server. The reported message is: ' +
                                        res.responseText;
                                } else {
                                    detail = 'An unexpected failure response was received from the server. The reported message is: ' +
                                        err.errorSource + ' : ' + err.errorMessage;
                                }
                            } catch (e) {
                                detail = 'An unexpected failure response was received from the server. The reported message is: ' +
                                    res.statusText + ' : ' + res.status;
                            }
                            debug.__error(detail);
                            visualization.__showError(self.name,
                                    detail);
                        }
                    }
                });
            } catch (x) {
                this.plots = undefined;
                if (self.__updateFooter()) {
                    self.__resize();
                }
                debug.__error(x);
                visualization.__showError(this.name, x);
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

                if (config.grouping !== undefined) {
                    request.grouping = config.grouping;
                }

                if (config.datapoints !== undefined) {
                    request.metrics = [];
                    config.datapoints
                            .forEach(function(dp) {
                                var m = {}, key;
                                if (dp.metric !== undefined) {
                                    m.metric = dp.metric;

                                    if (dp.rate !== undefined) {
                                        m.rate = dp.rate;
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

                                if (dp.downsample !== undefined) {
                                    m.downsample = dp.downsample;
                                }
                                if (dp.expression !== undefined) {
                                    m.expression = dp.expression;
                                }
                                // emit is deprecated, so ignore
                                // if (dp.emit !== undefined) {
                                //     m.emit = dp.emit;
                                // }
                                request.metrics.push(m);
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
            var self = this, plots = [], max = 0, i, result, dpi, dp, info, key, plot, tag, prefix;

            for (i in data.results) {
                result = data.results[i];

                /*
                 * The key for a series plot will be its distinguishing
                 * characteristics, which is the metric name and the tags. We will
                 * use any mapping from metric name to legend value that was part of
                 * the original request.
                 */
                info = self.plotInfo[result.metric];
                key = info.legend;
                if (result.tags !== undefined) {
                    key += '{';
                    prefix = '';
                    for (tag in result.tags) {
                        if (result.tags.hasOwnProperty(tag)) {
                            key += prefix + tag + '=' + result.tags[tag];
                            prefix = ',';
                        }
                    }
                    key += '};';
                }
                plot = {
                    'key' : key,
                    'color' : info.color,
                    'fill' : info.fill,
                    'values' : []
                };
                for (dpi in result.datapoints) {
                    dp = result.datapoints[dpi];
                    max = Math.max(Math.abs(dp.value), max);
                    plot.values.push({
                        'x' : dp.timestamp * 1000,
                        'y' : dp.value
                    });
                }
                plots.push(plot);
            }

            return [ plots, this.__calculateAutoScaleFactor(max) ];
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
            var results, plots, i, overlay, minDate, maxDate, plot, k, firstMetric;

            // NOTE: series is deprecated
            // if (data.series) {
            //     results = this.__processResultAsSeries(request, data);
            //     plots = results[0];
            // } else {
            //     results = this.__processResultAsDefault(request, data);
            //     plots = results[0];
            // }
            results = this.__processResultAsSeries(request, data);
            plots = results[0];

            // add overlays
            if (this.overlays.length && plots.length) {
                for (i in this.overlays) {
                    overlay = this.overlays[i];
                    // get the date range
                    firstMetric = plots[0];
                    plot = {
                        'key' : overlay.legend,
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
            return results;
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
                visualization.__showNoData(this.name);
            } else {
                visualization.__showChart(this.name);
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
                    $(this.div).height() - $(this.footer).outerHeight());
            this.closure = this.impl.build(this, data);
            this.impl.render(this);

            // If there is not data, let the user know
            if (!this.__havePlotData()) {
                visualization.__showNoData(this.name);
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
                    var i;
                    try {
                        i = visualization.chart;
                        self.config.type.split('.').forEach(function(seg) {
                            i = i[seg];
                        });
                        self.impl = i;
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
        }

    };

})();