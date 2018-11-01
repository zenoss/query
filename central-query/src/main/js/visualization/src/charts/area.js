(function() {
    "use strict";

    function Chart() {
        var _model = null;

        this.model = function(_) {
            if (!arguments.length) {
                return _model;
            }
            _model = _;
        };
    }

    var area = {
        required : {
            defined : 'nv',
            source : [ 'nv.d3.min.js', 'css/nv.d3.css', 'css/jquery-ui.css' ]
        },

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            // OK. Area charts really want data points to match up on keys,
            // which
            // makes sense as this is how they stack things. To make this work
            // we
            // going to walk the points and make sure they match
            __align(chart);

            var _chart = chart.closure,
                model = _chart.model();

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }

            // if we have a legendState stored,
            // use it to mark if a plot is disabled
            // so that graph will preserve series
            // toggle state
            if(model.legendState){
                // NOTE: legendState is an array of bools
                // that maps to the array of plots
                chart.plots.forEach(function(plot, i){
                    plot.disabled = model.legendState[i];
                });
            }

            chart.svg.datum(chart.plots)
                .transition()
                .duration(0)
                .call(model);

            this.styleThresholds(chart.$div);
        },

        resize : function(chart) {
            var _chart = chart.closure, model = _chart.model();
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width());
            chart.svg.transition().duration(0).call(model);

            var bigRect = chart.svg.select('.nv-stackedAreaChart > g > rect')[0][0];
            if (bigRect != null) {
                var availHeight = bigRect.height.animVal.value;
                var availWidth = bigRect.width.animVal.value;
            }

            chart.svg.select('.rm-interactive')
                .attr('height', availHeight)
                .attr('width', availWidth);
        },

        build : function(chart, data) {
            // OK. Area charts really want data points to match up on keys,
            // which makes sense as this is how they stack things. To make this work
            // we going to walk the points and make sure they match
            __align(chart);

            // create new area Chart
            var _chart = new Chart();
            var model = nv.models.stackedAreaChart();
            _chart.model(model);

            // override calculateResultsMax
            // with a stacked area specific method
            chart.calculateResultsMax = calculateResultsMax;

            model.useInteractiveGuideline(true);
            model.showControls(false);
            model.duration(0);
            model.showLegend(!chart.config.supressLegend);

            model.interactiveLayer.tooltip.keyFormatter(function(d) {
                var maxLength = 35;
                if (d.length > maxLength) {
                    d = d.substring(0,30) + "...";
                }
                return d;
            });

            // on legend state change, update any
            // overlays disabled state so they
            // can persist through graph refresh
            model.dispatch.on("stateChange", function(state){
                // state.disabled is an array of bools indicating
                // if a series is enabled or disabled.
                model.legendState = state.disabled;
            });

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);
            // since were controlling labels ourselves,
            // tell nvd3 not to try to format them
            model.headerFormatter(function(d) { return d; });

            // ensure that there are no duplicate ticks on the y axis
            model.yAxis.tickFormat(chart.dedupeYLabels(model));
            model.clipEdge(true);
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width() - 10);
            model.yAxis.axisLabel(chart.yAxisLabel);

            // set a different value formatter for tooltip values
            // than for axis labels
            model.valueFormatter(function(d){
                // the second arg to formatValue forces the value's
                // unit to be derived from the value instead of the
                // preferred unit set on the entire chart
                return chart.formatValue(d, true);
            });

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }

            // magic to make the yaxis label show up
            // see https://github.com/novus/nvd3/issues/17
            model.margin({left: 90});
            chart.svg.datum(chart.plots);

            nv.addGraph(function() {
                chart.svg.transition().duration(500).call(model);
                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                });

                var bigRect = chart.svg.select('.nv-stackedAreaChart > g > rect')[0][0];
                if (bigRect != null) {
                    var availHeight = bigRect.height.animVal.value;
                    var availWidth = bigRect.width.animVal.value;
                }

                chart.svg.select('.nv-stackedAreaChart > g')
                    .append('rect')
                    .attr('class', 'rm-interactive')
                        .attr('style', 'opacity:0')
                        .attr('height', availHeight)
                        .attr('width', availWidth)
                    .on("click", function (d, i) {
                            var bounds = this.getBoundingClientRect();
                            var tmStart = chart.normalizeTimeToMs(chart.config.range.start);
                            var tmEnd = chart.normalizeTimeToMs(chart.config.range.end);
                            // see 'magic' comment above to understand the -90
                            var pct = (d3.event.offsetX - 90) / bounds.width;
                            var tmDelta = (tmEnd - tmStart) * pct;
                            var tmClicked = tmStart + Math.floor(tmDelta);
                            chart.zoomTo(tmClicked);
                        });
            });

            return _chart;
        },
        render : function() {

        },

        // look for series' that are actually thresholds
        // and style them differently
        styleThresholds: function(el){
            $(el).find(".nv-series .nv-legend-text").each(function(i, legend){
                if(~$(legend).text().indexOf("*")){
                    legend.classList.add("threshold");
                }
            });
        }
    };



     /**
     * Culls the plots in a chart so that only data points with a common
     * time stamp remain.
     *
     * @param the
     *            chart that contains the plots to cull
     * @access private
     */
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

    /**
    * Aligns the plots in a chart so that data points within all plots
    * share common timestamps to allow proper stacking.
    *
    * @param chart the chart that contains the plots to align
    *
    * @access private
    */
    function __align(chart) {
        var data = chart.plots;
        var normalizedstamps = adjustedKeys(data);
        data.forEach(function (series) {
            // ---- series ----
            var pts = [];
            var highi = 0;
            normalizedstamps.forEach(function (outStamp) {
                // ---- datapoints ----
                var outValue;
                for (var i = highi; i < series.values.length; i++) {
                    if (series.values[i].x > outStamp) {
                        break; // found later timestamp; use previous value
                    }
                    highi = i;
                    outValue = series.values[i].y;
                }
                pts.push({ 'x': outStamp, 'y': outValue });
            });
            series.values = pts;
        });
    }

    function adjustedKeys(data) {
        var as = allStamps(data);
        var fe = firstEntry(data);
        return as.splice(as.indexOf(fe));
    }

    // returns array of all timestamps contained within all series
    function allStamps(data) {
        var unsorted = [];
        data.map(function (series) {
            series.values.map(function (entry) {
                unsorted.push(entry.x);
            });
        });
        var uniqs = spread(unsorted).sort();
        return uniqs;
    }

    function spread(array) {
        var uniqElems = [];
        array.forEach(function (elem) {
            if (uniqElems.indexOf(elem) < 0) {
                uniqElems.push(elem);
            }
        });
        return uniqElems;
    }

    // latest timestamp within all series will be first entry
    // of timestamp-aligned adjusted series
    function firstEntry(data) {
        var firsts = data.map(function (series) {
            return series.values[0].x;
        });
        return Math.max.apply(null, firsts);
    }

    /**
     * Accepts a query service api response and determines the maximum
     * value of all series datapoints in that response
     */
    function calculateResultsMax(data){
        var timeMap = {},
            max = 0;

        var accumulate = function(acc, val){
            return acc + val;
        };

        data.forEach(function(series){
            series.datapoints.forEach(function(dp){
                timeMap[dp.timestamp] = timeMap[dp.timestamp] || [];
                timeMap[dp.timestamp].push(dp.value || 0);
            });
        });

        for(var i in timeMap){
            max = Math.max(timeMap[i].reduce(accumulate), max);
        }

        return max;
    }


    $.extend(true, zenoss.visualization.chart, {
        area : area
    });

}());
