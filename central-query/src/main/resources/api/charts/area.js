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
            __cull(chart);

            var _chart = chart.closure,
                model = _chart.model();

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }

            // find out which series are disabled or not
            var disabled = {};
            chart.svg.selectAll("g.nv-series")
                .each(function(d) {
                    disabled[d.key] = d.disabled;
                });

            // make sure when we update we preserve the disabled behavior
            chart.plots.forEach(function(d) {
                if (disabled[d.key] === true) {
                    d.disabled = true;
                }
            });

            chart.svg.datum(chart.plots)
                .transition()
                .duration(0)
                .call(model);

            this.styleThresholds(chart.div);
        },

        resize : function(chart) {
            var _chart = chart.closure, model = _chart.model();
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width());
            chart.svg.transition().duration(0).call(model);
        },

        build : function(chart, data) {
            // OK. Area charts really want data points to match up on keys,
            // which
            // makes sense as this is how they stack things. To make this work
            // we
            // going to walk the points and make sure they match
            __cull(chart);

            // create new area Chart
            var _chart = new Chart();
            var model = nv.models.stackedAreaChart();
            _chart.model(model);

            // override calculateResultsMax
            // with a stacked area specific method
            chart.calculateResultsMax = calculateResultsMax;

            model.useInteractiveGuideline(true);
            model.showControls(false);

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

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
