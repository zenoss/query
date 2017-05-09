/**
 * Chart.js
 * main chart object
 */
(function () {
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
    var MAX_Y_AXIS_LABEL_LENGTH = 5;
    var DATE_FORMAT = (typeof Zenoss !== "undefined" && Zenoss.USER_DATE_FORMAT) || "MM/DD/YY";
    var UPDATE_TIMEOUT = 30000;

    // data for formatting time ranges
    var TIME_DATA = [
        {
            name: "minute",
            value: 60000,
            ticks: 4,
            // max number of units before we should go up a level
            breakpoint: 90,
            format: function (tz, d) {
                return moment.utc(d).tz(tz).format("HH:mm:ss");
            }
        }, {
            name: "hour",
            value: 3.6e+6,
            ticks: 4,
            breakpoint: 20,
            format: function (tz, d) {
                return moment.utc(d).tz(tz).format("HH:mm:ss");
            }
        }, {
            name: "day",
            value: 8.64e+7,
            ticks: 3,
            breakpoint: 7,
            format: function (tz, d) {
                return moment.utc(d).tz(tz).format(DATE_FORMAT + " HH:mm:ss");
            }
        }, {
            name: "week",
            value: 6.048e+8,
            ticks: 3,
            breakpoint: 4,
            format: function (tz, d) {
                return moment.utc(d).tz(tz).format(DATE_FORMAT + " HH:mm:ss");
            }
        }, {
            name: "month",
            value: 2.63e+9,
            ticks: 3,
            breakpoint: 13,
            format: function (tz, d) {
                return moment.utc(d).tz(tz).format(DATE_FORMAT + " HH:mm:ss");
            }
        }, {
            name: "year",
            value: 3.156e+10,

            ticks: 3,
            breakpoint: 1000,
            format: function (tz, d) {
                return moment.utc(d).tz(tz).format(DATE_FORMAT + " HH:mm:ss");
            }
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


    Chart = function (name, config) {
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

        this.printOptimized = config.printOptimized;

        // base should be something like 1000 or 1024
        this.base = config.base || 1000;

        // Build up a map of metric name to legend label.
        this.__buildPlotInfo();

        // Thresholds
        this.overlays = config.overlays || [];
        this.overlays.sort(utils.compareASC('legend'));

        this.projections = config.projections || [];
        this.projections.sort(utils.compareASC('id'));

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

        this.__renderCapacityFooter = config.renderCapacityFooter;
        this.__renderForecastingTimeHorizonFooter = config.renderForecastingTimeHorizonFooter;

        this.maxResult = undefined;
        this.minResult = undefined;

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

        __onDestroyed: function (e) {
            // check if the removed element is the chart container
            if (this.$div[0] === e.target) {
                if (typeof this.onDestroyed === "function") {
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
        formatValue: function (value, ignorePreferred, skipCalc) {
            /*
             * If we were given a undefined value, Infinity, of NaN (all things that
             * can't be formatted, then just return the value.
             */
            if (!$.isNumeric(value)) {
                return value;
            }

            return toEng(value, ignorePreferred ? undefined : this.preferredYUnit, this.format, this.base, skipCalc);
        },

        /**
         * Iterates over the list of data plots and sets up display information
         * about each plot, including its legend label, color, and if it is filled
         * or not.
         *
         * @access private
         */
        __buildPlotInfo: function () {
            var i, info, dp, nameOrMetric, key;
            var plotInfo = {};

            for (i = 0; i < this.config.datapoints.length; i++) {
                dp = this.config.datapoints[i];
                key = utils.shortId();
                dp.id = key;
                nameOrMetric = dp.name || dp.metric;
                info = {
                    'legend': dp.legend || nameOrMetric,
                    'color': dp.color,
                    'fill': dp.fill
                };
                plotInfo[key] = info;
            }

            this.getPlotInfo = function (d) {
                var metricId = d.id;
                if (metricId.endsWith('-raw')) {
                    metricId = metricId.replace('-raw', '');
                }
                return plotInfo[metricId] || {};
            };
        },

        /**
         * Checks to see if the passed in plot is actually an overlay.
         *
         * @access private
         * @param {object}
         *            plot the object representing the plot
         * @return boolean if the plot is an overlay
         */
        __isOverlay: function (plot) {
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
         */
        resize: function () {
            var theight, fheight, height, span;

            var $footer = this.$div.find(".zenfooter");
            var $title = this.$div.find(".graph_title");

            theight = parseInt($title.outerHeight(), 10);
            fheight = this.__hasFooter() ? parseInt($footer.outerHeight(), 10) : 0;
            height = parseInt(this.$div.height(), 10) - fheight - theight;
            span = $(this.message).find('span');

            // resize wrapper to ensure enough space for graph
            $(this.svgwrapper).outerHeight(height);

            if (this.impl) {
                this.impl.resize(this);
            }

            $(this.message).outerHeight(height);
            span.css('margin-top', -parseInt(span.height(), 10) / 2);
        },

        /**
         * Constructs and appends a footer row onto the footer table
         *
         * @access private
         */
        __appendFooterRow: function () {
            var tr, td, d, i;

            tr = document.createElement('tr');
            $(tr).addClass('zenfooter_value_row');

            // One column for the color
            td = document.createElement('td');
            $(td).addClass('zenfooter_box_column');
            d = document.createElement('div');
            $(d).addClass('zenfooter_box');
            $(d).css('backgroundColor', 'transparent');
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

        __getAssociatedPlot: function (dp) {
            var i, ll;
            if (!this.plots) {
                return undefined;
            }

            ll = this.plots.length;
            for (i = 0; i < ll; i += 1) {
                if (dp.legend && (
                    this.plots[i].key === (dp.legend || dp.metric) ||
                    this.plots[i].key === dp.legend + '*' // thresholds
                    )) {
                    return this.plots[i];
                }
            }
            return undefined;
        },

        __redrawLowerLegend: function() {
            // Set the CSS based on the disabled property for the legend boxes.
            var rows, ll, cols, box, color, plot;
            rows = $(this.table).find('tr.zenfooter_value_row');
            ll = this.plots.length;
            for (var i = 0; i < ll; i++) {
                cols = $(rows[i]).find('td');
                box = $(cols[0]).find('div.zenfooter_box');
                if (this.impl) {
                    color = this.impl.color(this, this.closure, i);
                } else {
                    // unable to determine color
                    color = {
                        color: "white",
                        opacity: 1
                    };
                }
                plot = this.plots[i];
                if (plot.color) {
                    color.color = plot.color;
                }
                box.css('background-color', plot.disabled ? 'transparent' : color.color);
                box.css('opacity', color.opacity);
            }

            // Refresh the graph.
            this.impl.resize(this);
        },

        /**
         * Called when a lower Legend on the graph is clicked.
         */
        __lowerLegendClicked: function (dp) {
            // Get the associated plot and toggle the enabled flag.
            var plot = this.__getAssociatedPlot(dp);
            plot.disabled = !plot.disabled;

            // If all elements are disabled, enable them all.
            if (this.plots.every(function(p) { return p.disabled; })) {
                this.plots.forEach(function(p) { p.disabled = false; });
            }

            this.__redrawLowerLegend();
        },

        /**
         * Called when a lower Legend on the graph is double-clicked.
         */
        __lowerLegendDblClicked: function(dp) {
            // Double click on a datapoint causes it to be enabled
            // and all others to be disabled.
            var plot = this.__getAssociatedPlot(dp);
            this.plots.forEach(function(p) {
                p.disabled = (p !== plot);
            });
            this.overlays.forEach(function(o) {
                o.disabled = (o !== plot);
            });

            this.__redrawLowerLegend();
        },

        /**
         * Called when the mouse hovers over a lower Legend item.
         */
        __lowerLegendMouseOver: function(dp) {
            var plot = this.__getAssociatedPlot(dp);

            this.svg.selectAll('.nv-group').classed( {
                'zenchart_lowlight':  function(d) {
                    return (d !== plot);
                },
                'zenchart_spotlight': function(d) {
                    return (d === plot);
                }
            });
        },

        /**
         * Called when the mouse leaves a lower Legend item.
         */
        __lowerLegendMouseOut: function() {
            /**
            * Restore the opacity/stroke-width from the mouseover for all series.
            */
            this.svg.selectAll('.nv-group').classed({'zenchart_lowlight': false, 'zenchart_spotlight': false});
        },

        /**
         * Add the events to the table row.
         */
        __setLegendEvents: function(tr, dp) {
            (function(chart) {
                tr.addEventListener('click', function() {
                    chart.__lowerLegendClicked(dp);
                }, false);
                tr.addEventListener('dblclick', function() {
                    chart.__lowerLegendDblClicked(dp);
                }, false);
                tr.addEventListener('mouseover', function() {
                    chart.__lowerLegendMouseOver(dp);
                }, false);
                tr.addEventListener('mouseout', function() {
                    chart.__lowerLegendMouseOut();
                }, false);
                // Prevent highlighting on the double-click event.
                tr.addEventListener('mousedown', function (event) {
                    if (event.detail > 1) {
                        event.preventDefault();
                    }
                }, false);
            })(this);
        },

        /**
         * Updates the chart footer based on updated data. This includes adding or
         * removing footer rows as well as filling in colors and data.
         *
         * @access private
         * @return true if the changes to the footer necesitates a resize of the
         *         chart, else false.
         */
        __updateFooter: function (data) {
            var sta, eta, plot, dp, vals, cur, min, max, avg, cols, init, label, ll, i, v, vIdx, k, rows, row, box, color, resize = false,
                timezone = this.timezone, tr;
            if (!this.table) {
                return false;
            }

            rows = $(this.table).find('tr');
            if (data) {
                sta = this.dateFormatter(data.startTimeActual, timezone);
                eta = this.dateFormatter(data.endTimeActual, timezone);
            } else {
                sta = eta = "N/A";
            }
            $($(rows[0]).find('td')[0]).html(
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
                for (i = 0; i < this.config.datapoints.length; i++) {
                    dp = this.config.datapoints[i];
                    plot = this.__getAssociatedPlot(dp);
                    if (!this.__isOverlay(dp.legend || dp.metric) &&
                        (dp.emit === undefined || dp.emit)) {
                        if (row >= rows.length) {
                            tr = this.__appendFooterRow();
                            rows.push(tr);
                            resize = true;
                            this.__setLegendEvents(tr[0], dp);
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
                                color: "transparent",
                                opacity: 1
                            };
                        }

                        if (dp.color) {
                            color.color = dp.color;
                        }
                        box = $(cols[0]).find('div.zenfooter_box');
                        box.css('background-color', color.color);
                        box.css('border-color', color.color);
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
                            vals = [0, 0, 0, 0];
                            cur = 0;
                            min = 1;
                            max = 2;
                            avg = 3;
                            init = false;

                            for (vIdx = 0; vIdx < plot.values.length; vIdx++) {
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

                            if (isFinite(this.maxResult[row])) {
                                vals[max] = this.maxResult[row];
                            }
                            if (isFinite(this.minResult[row])) {
                                vals[min] = this.minResult[row];
                            }

                            for (v = 0; v < vals.length; v += 1) {
                                $(cols[2 + v]).html(this.formatValue(vals[v], undefined, dp.displayFullValue));
                            }
                        }
                        row += 1;
                    }
                }
            }

            // remove any extra rows
            if (row < rows.length) {
                // includes overlay header row
                for (i = rows.length-1; i >= row-1; i--) {
                    rows.splice(-1,1);
                    $(this.table).find("tr:last").remove();
                }
                resize = true;
            }

            // Add thresholds
            if (this.config.overlays && this.config.overlays.length) {
                // One row for the stats table header
                tr = document.createElement('tr');
                $(tr).addClass("zenfooter_tablerow_header");
                tr.innerHTML = '<th class="footer_header zenfooter_box_column"></th>' +
                    '<th class="footer_header zenfooter_data_text" colspan="5">Thresholds</th>';
                $(this.table).append($(tr));
                rows.push($(tr));

                for (i = 0; i < this.config.overlays.length; i++) {
                    dp = this.config.overlays[i];
                    row = rows.length;

                    if (row >= rows.length) {
                        tr = this.__appendFooterRow();
                        rows.push(tr);
                        resize = true;
                        this.__setLegendEvents(tr[0], dp);
                    }

                    cols = $(rows[row]).find('td');

                    // footer color
                    if (dp.color) {
                        color.color = dp.color;
                    } else if (this.impl) {
                        color = this.impl.color(this, this.closure, i + ll);
                    } else {
                        // unable to determine color
                        color = {
                            color: "transparent",
                            opacity: 1
                        };
                    }

                    // color box
                    $(cols[0])
                        .find('div.zenfooter_box')
                        .css('background-color', color.color)
                        .css('border-color', color.color)
                        .css('opacity', color.opacity);

                    // Threshold
                    label = dp.legend + '*';
                    $(cols[1])
                        .html(label)
                        .attr('colspan','8') // 5 + 3 projection cells
                        .addClass('zenfooter_threshold');
                }
            }

            if (this.__renderCapacityFooter !== undefined) {
                this.__renderCapacityFooter(this);
            }
            if (this.__renderForecastingTimeHorizonFooter !== undefined) {
                this.__renderForecastingTimeHorizonFooter(this);
            }

            if (this.__getProjectionPlots().length > 0) {
                this.__renderProjectionFooter();
            }

            return resize;
        },
        /**
         * Returns all the current chart's plots that are of type projection
         *
         * @access private
         * @return [object] all the projection plots
         */
        __getProjectionPlots: function () {
            return $.grep(this.plots, function (p) {
                return p.projection;
            });
        },
        /**
         * Renders the projection legend with a mouse over of future dates
         * @access private
         **/
        __renderProjectionFooter: function () {
            // first remove all previous projections
            $(this.footer).find(".zenfooter_projection").remove();
            var tableRows = $(this.footer).find('tr');
            var titleRow = $(tableRows[0]);
            var headerRow = tableRows[1];

            var projections = this.__getProjectionPlots(),
            // the days out that we are showing projections for (e.g. 30 days from now)
                futureTimes = [30, 60, 90];
            projections.sort(utils.compareASC('key'));

            titleRow.append($("<td/>",{
                text: "",
                class: "zenfooter_lined_spacer zenfooter_projection"
            }));
            // append title header
            titleRow.append($("<td/>",{
                colspan: futureTimes.length,
                text: "Projected Values",
                class: "zenfooter_dates zenfooter_projection"
            }));

            $(headerRow).append($("<td/>",{
                text: "",
                class: "zenfooter_lined_spacer zenfooter_projection"
            }));
            //append column headers
            for(var i=0; i<futureTimes.length; ++i) {
                var futureTime = moment().add(futureTimes[i], 'days');
                var newColumn = $("<th/>", {
                    text: futureTime.format("MMM-D") + " (" + futureTimes[i].toString() + " days)",
                    class: 'footer_header zenfooter_data_text zenfooter_projection'
                });
                $(headerRow).append(newColumn);
            }

            // append column data
            var duplicateRowKeys = {};
            projections.forEach(function (projection) {
                var i, futureTime, rawProjectedValue, projectedValue;
                // append row spacer
                var rowKey = projection.key.replace("Projected ", "").split(" - ")[0];

                // forecasting plot data comes in the same order as the graph footer data
                // we need to keep track of seen row names because duplicate names are possible
                // in that case, we rely on the order that we've seen the data
                if(duplicateRowKeys[rowKey] !== undefined) {
                    duplicateRowKeys[rowKey]++;
                }else{
                    duplicateRowKeys[rowKey] = 0;
                }
                var rowIndex = duplicateRowKeys[rowKey];

                var row = $($(this.footer).find('.zenfooter_data_text:contains(' + rowKey + ')').get(rowIndex)).parent();
                row.append($("<td/>",{
                    text: "",
                    class: "zenfooter_lined_spacer zenfooter_projection"
                }));

                for (i = 0; i < futureTimes.length; i++) {
                    futureTime = moment().add(futureTimes[i], 'days');
                    rawProjectedValue = Number(projection.projectionFn(futureTime.unix()).toFixed(2));
                    projectedValue = rawProjectedValue > 0 ? this.formatValue(rawProjectedValue) : 0;

                    var projectionColumn = $("<td/>", {
                        text: projectedValue,
                        class: "zenfooter_data zenfooter_data_number zenfooter_projection"
                    });

                    $(row).append(projectionColumn);
                }
            }.bind(this));

            this.resize();
        },
        /**
         * Returns true if this chart is displaying a footer, else false
         *
         * @access private
         * @return true if this chart is displaying a footer, else false
         */
        __hasFooter: function () {
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
        __footerRangeOnly: function () {
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
        __buildFooter: function (config, data) {
            var tr, td, dates;
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
                $(tr).addClass("zenfooter_tablerow_header");
                tr.innerHTML = '<th class="footer_header zenfooter_box_column"></th>' +
                    '<th class="footer_header zenfooter_data_text">Metric</th>' +
                    '<th class="footer_header zenfooter_data_number">Last</th>' +
                    '<th class="footer_header zenfooter_data_number">Min</th>' +
                    '<th class="footer_header zenfooter_data_number">Max</th>' +
                    '<th class="footer_header zenfooter_data_number">Avg</th>';
                $(this.table).append($(tr));
            }

            // Fill in the stats table
            this.__updateFooter(data);
        },
        hasPendingRequests: function(){
            return this.updatePromise && this.updatePromise.state() == "pending";
        },
        cancelUpdate: function() {
            // cancel ajax request (async req)
            this.updateRequest.abort();
            this.cleanupDataReq();
        },
        cleanupDataReq: function() {
            clearTimeout(this.updateTimeout);
            this.updateTimeout = null;
        },

        /**
        * Update this.maxResult array that will be using in building the legend.
        * @access private
        * @param {object}
        *     arr(ay) of max values
        * return this.maxResult
        */
        __updateMaxResult: function (arr) {
            this.maxResult = arr;
            return this.maxResult;
        },
        /**
        * Update this.minResult array that will be using in building the legend.
        * @access private
        * @param {object}
        *     arr(ay) of min values
        * return this.minResult
        */
        __updateMinResult: function (arr) {
            this.minResult = arr;
            return this.minResult;
        },
        /**
        * Get max values for the period and pass them to the __updateMaxResult
        * @access private
        * @param {object}
        *     data from the maxValueRequest
        */
        __maxValues: function (data) {
            var i, j, maxDataResults, maxValues = [];
            var maxResult = [];
            for (i = 0; i < data.results.length; i++) {
                 maxDataResults = data.results[i].datapoints;
                 if (maxDataResults !== undefined){
                     for (j = 0; j < maxDataResults.length; j++) {
                         maxValues.push(maxDataResults[j].value);
                      }
                      maxResult.push(Math.max.apply(null, maxValues));
                      maxValues = [];
                 }
            }
            this.__updateMaxResult(maxResult);
        },
        /**
        * Get min values for the period and pass them to the __updateMinResult
        * @access private
        * @param {object}
        *     data from the minValueRequest
        */
        __minValues: function (data) {
            var i, j, minDataResults, minValues = [];
            var minResult = [];
            for (i = 0; i < data.results.length; i++) {
                minDataResults = data.results[i].datapoints;
                if (minDataResults !== undefined){
                    for (j = 0; j < minDataResults.length; j++) {
                        minValues.push(minDataResults[j].value);
                    }
                    minResult.push(Math.min.apply(null, minValues));
                    minValues = [];
                }
            }
            this.__updateMinResult(minResult);
        },
        /**
         * Updates a graph with the changes specified in the given change set. To
         * remove a value from the configuration its value should be set to a
         * negative sign, '-'.
         *
         * @param {object}
         *            changeset updates to the existing graph's configuration.
         */
        update: function (changeset) {
            if(this.hasPendingRequests()){
                // do nothing, waiting for the results
                return;
            }
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
            kill.forEach(function (p) {
                delete self.config[p];
            });

            /*
             * Rebuild the legend and color tables
             */
            this.__buildPlotInfo();

            try {
                this.request = this.__buildDataRequest(this.config);
                this.maxRequest = jQuery.extend({}, this.request);
                if (this.maxRequest.downsample !== null) {
                    this.maxRequest.downsample = this.maxRequest.downsample.replace("avg", "max");
                }
                var maxValueRequest = $.ajax({
                    'url': visualization.url + visualization.urlPerformance,
                    'type': 'POST',
                    'data': JSON.stringify(this.maxRequest),
                    'dataType': 'json',
                    'contentType': 'application/json'
                });
                this.minRequest = jQuery.extend({}, this.request);
                if (this.minRequest.downsample !== null) {
                    this.minRequest.downsample = this.minRequest.downsample.replace("avg", "min");
                }
                var minValueRequest = $.ajax({
                    'url': visualization.url + visualization.urlPerformance,
                    'type': 'POST',
                    'data': JSON.stringify(this.minRequest),
                    'dataType': 'json',
                    'contentType': 'application/json'
                });
                this.updateRequest = $.ajax({
                    'url': visualization.url + visualization.urlPerformance,
                    'type': 'POST',
                    'data': JSON.stringify(this.request),
                    'dataType': 'json',
                    'contentType': 'application/json'
                });

                $.when(maxValueRequest, minValueRequest, this.updateRequest)
                    .then(function(response1, response2, response3) {
                        var data = response3[0];
                        self.__maxValues(response1[0]);
                        self.__minValues(response2[0]);

                        self.plots = self.__processResult(self.request, data);

                        // setPreferred y unit (k, G, M, etc)
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
                            self.resize();
                        }

                        // send a separate request for the projection data since it has a different time span
                        var projectionColors = ["#EBEBEF", "#FDDFE7", "#FCF1C0", "#DAFBEB"], projectionIndex = 0;
                        self.projections.forEach(function (projection) {
                            var projectionRequest = self.__buildProjectionRequest(self.config, self.request, projection);
                            // can fail if the projection is requesting a metric not present
                            if (!projectionRequest) {
                                return;
                            }
                            $.ajax({
                                'url': visualization.url + visualization.urlPerformance,
                                'type': 'POST',
                                'data': JSON.stringify(projectionRequest),
                                'dataType': 'json',
                                'contentType': 'application/json',
                                'success': function (projectionData) {

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
                                'error': function () {
                                    // the trendline isn't critical to the graph so
                                    // do nothing in the case of errors
                                }
                            });
                        });
                    });
                this.updatePromise = $.when(this.updateRequest);
                if(this.onUpdate){
                    // if we have access to the onUpdate function of a graph, send it the ajax request promise
                    this.onUpdate(this.updatePromise);
                }
                // set timeout for update promise
                this.updateTimeout = setTimeout(this.cancelUpdate.bind(this), UPDATE_TIMEOUT);
                this.updatePromise.then(function(){
                    self.cleanupDataReq();
                },
                function (err) {
                    if(err.statusText == "abort"){
                        // if the status text reads "abort" we have cancelled a request that took too long
                        self.__showTimeout();
                    } else {
                        self.__showNoData();
                    }
                    self.plots = [];
                    self.cleanupDataReq();

                    // upon errors still show the footer
                    if (self.showLegendOnNoData && self.__hasFooter()) {
                        // if this is the first request that errored we will need to build the table
                        if (!self.table) {
                            self.__buildFooter(self.config);
                        } else {
                            if (self.__updateFooter()) {
                                self.resize();
                            }
                        }
                    }
                });

            } catch (x) {
                // set plots to an empty array so we can append to it later
                this.plots = [];
                if (self.__updateFooter()) {
                    self.resize();
                }
                debug.__error(x);
                this.__showError(x);
            }
        },
        /**
         *  Converts a downsample rate into a "step". To minimized clutter each step is a multiple
         *  the downsample rate.
         **/
        __convertDownsampletoStep: function (downsample) {
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
        createRegressionData: function (projectionFn, start, end) {
            var regression = [],
                downsample = this.request.downsample,
                config = this.config,
                y, skipThisPoint = false,
                step = this.__convertDownsampletoStep(downsample), t = start;
            while (t < end) {
                y = projectionFn(t);
                // make sure it is always visible in the graph (does not go below miny)
                if (config.miny !== undefined && config.miny !== null && y <= config.miny) {
                    y = config.miny;
                    skipThisPoint = true;
                }

                // make sure it doesn't go above maxy
                if (config.maxy !== undefined && config.maxy !== null && y >= config.maxy) {
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
        createRegressionFunction: function (projection, values) {
            // get the implementation based on the projection "type" (or projectionAlgorithm property)
            var xValues = $.map(values, function (o) {
                    return o.timestamp;
                }),
                yValues = $.map(values, function (o) {
                    return o.value;
                });

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
        __buildDataRequest: function (config) {
            var request = {};

            if (config !== undefined) {

                if (config.range) {
                    if (config.range.start) {
                        request.start = config.range.start;
                    }

                    if (config.range.end) {
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
                    request.downsample = DOWNSAMPLE.reduce(function (acc, val) {
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
                        .forEach(function (dp) {
                            var m = {}, key, expressionMetric;
                            if (dp.metric !== undefined) {
                                m.metric = dp.metric;

                                if (dp.id) {
                                    m.id = dp.id;
                                }

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
                                    expression: dp.expression.replace("rpn:", "rpn:" + m.name + "-raw,"),
                                    id: m.id
                                };

                                // original datapoint is now just a vehicle for the
                                // expression to evaluate against. Rename with -raw suffix as that is the default
                                // used by zenoss to self reference a datapoint in an RPN
                                m.emit = false;
                                m.name = m.name + "-raw";
                                m.id = m.id + "-raw";
                            }

                            request.metrics.push(m);

                            // if an expressionMetric was created, add to request
                            if (expressionMetric) {
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
        __buildProjectionRequest: function (config, dataRequest, projection) {
            var request = {
                metrics: []
            }, start, end, delta, self = this;
            if (!projection.metric) {
                return false;
            }
            dataRequest.metrics.forEach(function (m) {
                var metric, test = m.metric || m.name;
                // use the datapoint name for the case of rpn metrics
                if (test.indexOf(projection.metric.split("_")[1]) != -1) {
                    // copy of the object
                    metric = $.extend(true, {}, m);
                    metric.aggregator = projection.aggregateFunction || "max";
                    metric.emit = true;
                    if (self.getPlotInfo(metric)) {
                        projection.legend = "Projected " + self.getPlotInfo(metric).legend + " - " + projection.id;
                    }
                    request.metrics.push(metric);
                }
            });
            if (!request.metrics.length) {
                return false;
            }

            request.returnset = config.returnset;
            request.tags = dataRequest.tags;
            request.end = parseInt(new Date().getTime() / 1000); // now
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
                request.downsample = DOWNSAMPLE.reduce(function (acc, val) {
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
        __processResultAsSeries: function (request, data) {

            var plots = [],
                start = data.startTimeActual,
                end = data.endTimeActual,
                drange = end - start,
            // allowable deviation expected start/end points
                drangeDeviation = drange * 0.2;

            data.results.forEach(function (series) {

                var info, key, plot;

                // if series.datapoints is not defined, or there are no points
                if (!series.datapoints || (series.datapoints && !series.datapoints.length)) {
                    series.datapoints = [{
                        timestamp: start,
                        value: null
                    }, {
                        timestamp: end,
                        value: null
                    }];
                }

                // ensure the series starts at the expected time (or near it at least)
                if (series.datapoints[0].timestamp !== start && series.datapoints[0].timestamp - start > drangeDeviation) {
                    series.datapoints.unshift({
                        timestamp: start,
                        value: null
                    });
                }
                // ensure the series ends at the expected time (or near it at least)
                if (series.datapoints[series.datapoints.length - 1].timestamp !== end &&
                    (end - series.datapoints[series.datapoints.length - 1].timestamp) > drangeDeviation) {
                    series.datapoints.push({
                        timestamp: end,
                        value: null
                    });
                }


                // create plots from each datapoint
                info = this.getPlotInfo(series);
                key = info.legend;
                // TODO - use tags to make key unique
                plot = {
                    'key': key,
                    'color': info.color,
                    'fill': info.fill,
                    'values': [],
                    'disabled': info.disabled
                };

                series.datapoints.forEach(function (datapoint) {
                    plot.values.push({
                        x: datapoint.timestamp * 1000,
                        // ensure value is a number
                        y: typeof datapoint.value !== "number" ? null : datapoint.value
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
        __processResult: function (request, data) {
            var plots, firstMetric, minDate, maxDate;

            plots = this.__processResultAsSeries(request, data);

            // get the date range
            firstMetric = plots[0];
            minDate = firstMetric.values[0].x;
            maxDate = firstMetric.values[firstMetric.values.length - 1].x;

            // add overlays
            if (this.overlays.length && plots.length && plots[0].values.length) {
                this.overlays.forEach(function (overlay) {
                    // if disabled is undefined, default to true, otherwise
                    // use the disabled value
                    var isDisabled = "disabled" in overlay ? overlay.disabled : false;

                    // if the overlay includes 2 values, we assume
                    // it is a minmax threshold
                    var isMinMax = overlay.values.length === 2 ? true: false;

                    // NOTE: each value in an overlay is used to draw a horizontal
                    // line at that y coordinate.
                    overlay.values.sort().forEach(function(value, k){
                        var plot = {
                            'key': overlay.legend + "*",
                            'disabled': isDisabled,
                            'values': [],
                            'color': overlay.color,
                            // store original overlay object
                            // on this plot
                            'overlay': overlay
                        };

                        // create a line by putting a point at the start and a point
                        // at the end
                        plot.values.push({
                            x: minDate,
                            y: value
                        });
                        plot.values.push({
                            x: maxDate,
                            y: value
                        });

                        // if this is a minmax threshold, we want
                        // to label the key as such
                        if(isMinMax){
                            if(k === 0){
                                plot.key = overlay.legend +" min*";
                            } else if(k === 1){
                                plot.key = overlay.legend +" max*";
                            }
                        }
                        plots.push(plot);
                    });
                });
            }

            return plots;
        },

        /**
         * Returns true if the chart has plots and they contain data points, else
         * false.
         *
         * @access private
         */
        __havePlotData: function () {
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
        __updateData: function (data) {

            if (!this.__havePlotData()) {
                this.__showNoData();
            } else {
                this.__showChart();
                this.impl.update(this, data);
            }

            if (this.__updateFooter(data)) {
                this.resize();
            }
        },

        /**
         * Constructs a chart from the given data
         *
         * @param data
         *            the data returned from a metric query
         * @access private
         */
        __buildChart: function (data) {
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
        __render: function (data) {
            var self = this;
            dependency.__loadDependencies({
                'defined': self.config.type.replace('.', '_'),
                'source': ['charts/' + self.config.type.replace('.', '/') + '.js']
            }, function () {
                var impl;
                try {
                    impl = visualization.chart;
                    self.config.type.split('.').forEach(function (seg) {
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
                dependency.__loadDependencies(self.impl.required, function () {
                    self.__buildChart(data);
                    if (self.__hasFooter()) {
                        self.__buildFooter(self.config, data);
                    }
                    self.resize();

                    if (self.afterRender) {
                        setTimeout(function () {
                            self.afterRender();
                        }, 0);
                    }

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
        __showError: function (detail) {
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
        __showNoData: function () {
            this.__showMessage('<span class="nodata"></span>');
        },

        __showTimeout: function () {
            this.__showMessage('<span class="timeout"></span>');
        },

        __hideMessage: function () {
            this.$div.find(".message").css('display', 'none');
        },

        __showMessage: function (message) {
            // cache some commonly used selectors
            var $message = this.$div.find(".message");
            //var $messageSpan = $message.find("span");

            if (message) {
                $message.html(message);
            }
            this.__hideChart();

            $message.css('display', 'block');
        },

        __hideChart: function () {
            this.$div.find('.zenchart').css('display', 'none');

            if (!this.showLegendOnNoData) {
                this.$div.find('.zenfooter').css('display', 'none');
            }
        },

        __showChart: function () {
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
        dateFormatter: function (date, timezone) {
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
         * @default "MM/DD/YY HH:mm:ss"
         */
        dateFormat: DATE_FORMAT + " HH:mm:ss",

        // uses TIME_DATA to determine which time range we care about
        // and format labels representative of that time range
        updateXLabels: function (start, end, axis) {
            var dateRange = end - start,
                done, timeFormat;

            // figure out which unit we care about
            TIME_DATA.forEach(function (timeFormatObj) {
                if (!done && dateRange <= timeFormatObj.value * timeFormatObj.breakpoint) {
                    timeFormat = timeFormatObj;
                    done = true;
                }
            });

            // set number of ticks based on unit
            axis.ticks(timeFormat.ticks)
                .tickFormat(timeFormat.format.bind(null, this.timezone));
        },

        dedupeYLabels: function (model) {
            var prevY;

            return function (value, index) {
                var yDomain = model.yDomain() || [0, 1],
                    formatted = this.formatValue(value),
                // min and max labels do not have an index set
                // where regular labels do
                    isMinMax = index === undefined ? true : false;

                // if prevY hasn't been set yet, this is
                // the first time this has been run, so
                // set it.
                if (prevY === undefined) {
                    prevY = this.formatValue(yDomain[0]);
                }

                // if this is not the min/max tick, and matches the previous
                // tick value, the min tick value or the max tick value,
                // do not return a tick value (I'm sure that's crystal
                // clear now)
                if (!isMinMax && (formatted === prevY ||
                    formatted === this.formatValue(yDomain[0]) ||
                    formatted === this.formatValue(yDomain[1]))) {
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
        calculateYDomain: function (miny, maxy, data) {
            // if max is not provided, calcuate max
            if (maxy === undefined) {
                maxy = this.calculateResultsMax(data.results);
            }

            // if min is not provided, calculate min
            if (miny === undefined) {
                miny = this.calculateResultsMin(data.results);
            }

            // if min and max are the same, add a bit to
            // max to separate them
            if (miny === maxy) {
                maxy += maxy * 0.1;
            }

            // if min and max are zero, force a
            // 0,1 domain
            if (miny === 0 && maxy === 0) {
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
        calculateResultsMin: function (data, nonZero) {
            // if nonZero, set things up to start from Infinity
            var minStartValue = nonZero ? Infinity : 0,
                result;

            result = data.reduce(function (acc, series) {
                return Math.min(acc, series.datapoints.reduce(function (acc, dp) {
                    // if the value is the string "NaN", ignore this dp
                    if (dp.value === "NaN" || dp.value === null) return acc;
                    if (nonZero && dp.value === 0) return acc;
                    return Math.min(acc, +dp.value);
                }, minStartValue));
            }, minStartValue);

            // if the result is Infinity, then all the values
            // were zero, so just return zero
            if (result === Infinity) {
                result = 0;
            }

            return result;
        },

        /**
         * Accepts a query service api response and determines the maximum
         * value of all series datapoints in that response
         */
        calculateResultsMax: function (data) {
            // extract array of value arrays
            var seriesVals = data.map(function (series) {
                return series.datapoints.map(function (datapt) { 
                    return datapt.value === "NaN" ? 0 : +datapt.value; });
            });
            // flatten array and calculate max value
            return Math.max.apply(null, [].concat.apply([], seriesVals));
        },

        setPreferredYUnit: function (data) {
            var val = this.calculateResultsMax(data),
                x, unitIndex;

            // if maxy is set, constrain the value based on that
            if (this.maxy !== undefined) {
                val = this.maxy;
            }

            // if miny is set and val is less than miny, set val to miny
            if (this.miny !== undefined && val < this.miny) {
                val = this.miny;
            }

            if (val === 0) {
                unitIndex = 0;
            } else {
                x = Math.log(Math.abs(val)) / Math.log(this.base);
                unitIndex = Math.floor(x);
            }

            this.preferredYUnit = unitIndex;
        },

        // returns date object for start time of this chart
        getStartDate: function () {
            if (!this.request.start) {
                console.warn("Missing start date");
                return;
            }
            var date = utils.createDate(this.request.start);
            return date;
        },
        getEndDate: function () {
            if (!this.request.end) {
                console.warn("Missing end date");
                return;
            }
            var date = utils.createDate(this.request.end);
            return date;
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

    function toEng(val, preferredUnit, format, base, skipCalc) {
        var result,
            unit,
            formatted,
            symbol,
            targetLength;

        // check if we want to provide magnitude calculation
        if (!skipCalc) {
            // if preferredUnit is provided, target that value
            if (preferredUnit !== undefined) {
                unit = preferredUnit;
            } else if (val === 0) {
                unit = 0;
            } else {
                unit = Math.floor(Math.log(Math.abs(val)) / Math.log(base));
            }

            symbol = SYMBOLS[unit];
            targetLength = MAX_Y_AXIS_LABEL_LENGTH;

            // TODO - if Math.abs(unit) > 8, return value in scientific notation
            result = val / Math.pow(base, unit);
        } else {
            result = val;
            symbol = "";
            targetLength = String(val).length;
        }

        try {
            // if sprintf is passed a format it doesn't understand an exception is thrown
            formatted = sprintf(format, result);
        } catch (err) {
            console.error("Invalid format", format, "using default", DEFAULT_NUMBER_FORMAT);
            formatted = sprintf(DEFAULT_NUMBER_FORMAT, result);
        }

        // TODO - make graph y axis capable of expanding to
        // accommodate long numbers
        return shortenNumber(formatted, targetLength) + symbol;
    }

    // attempts to make a long floating point number
    // fit in `length` characters. It will trim the
    // fractional part of the number, but never the
    // whole part of the number
    function shortenNumber(numStr, targetLength) {
        var parts = numStr.split("."),
            whole = parts[0],
            fractional = parts[1] || "";

        // if the number is already short enough
        if (whole.length + fractional.length <= targetLength) {
            return numStr;
        }

        // if the whole part of the number is
        // too long, return it as is. we tried our best.
        if (whole.length >= targetLength) {
            return whole;
        }

        return whole + "." + fractional.substring(0, targetLength - whole.length);
    }
})();
