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
            source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
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
                model.yDomain(calculateYDomain(chart.miny, chart.maxy, data));
            }

            chart.svg.datum(chart.plots).transition().duration(0).call(
                model);
        },

        resize : function(chart) {
            var _chart = chart.closure, model = _chart.model();
            model.height($(chart.svgwrapper).height());
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

            // disable advanced area controls
            model.controlsData([]);

            model.useInteractiveGuideline(true);

			chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            model.yAxis.tickFormat(function(value){
                return chart.formatValue(value);
            });
            model.clipEdge(true);
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width() - 10);
            model.yAxis.axisLabel(chart.yAxisLabel);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(calculateYDomain(chart.miny, chart.maxy, data));
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
     * Create y domain based on options and calculated data range
     */
    function calculateYDomain(miny, maxy, data){
        // if max is not provided, calcuate max
        if(maxy === undefined){
            maxy = calculateResultsMax(data.results);
        }

        // if min is not provided, calculate min
        if(miny === undefined){
            miny = calculateResultsMin(data.results);
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
    }

    /**
     * Accepts a query service api response and determines the minimum
     * value of all series datapoints in that response
     */
    function calculateResultsMin(data){
        return data.reduce(function(acc, series){
            return Math.min(acc, series.datapoints.reduce(function(acc, dp){
                return Math.min(acc, +dp.value);
            }, 0));
        }, 0);
    }

    /**
     * Accepts a query service api response and determines the maximum
     * value of all series datapoints in that response
     */
    function calculateResultsMax(data){

        var seriesCalc = function(a,b){
            return a+b;
        };

        return data.reduce(function(acc, series){
            return seriesCalc(acc, series.datapoints.reduce(function(acc, dp){
                return Math.max(acc, +dp.value);
            }, 0));
        }, 0);
    }


    $.extend(true, zenoss.visualization.chart, {
        area : area
    });

}());
