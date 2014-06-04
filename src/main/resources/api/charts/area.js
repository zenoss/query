(function() {
    "use strict";
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

        Chart : function() {
            var _model = null;

            this.model = function(_) {
                if (!arguments.length) {
                    return _model;
                }
                _model = _;
            };
        },

        update : function(chart, data) {
            // OK. Area charts really want data points to match up on keys,
            // which
            // makes sense as this is how they stack things. To make this work
            // we
            // going to walk the points and make sure they match
            __cull(chart);

            var _chart = chart.closure;

            _chart.model().xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });

            chart.svg.datum(chart.plots).transition().duration(0).call(
                    _chart.model());
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

            var _chart = new zenoss.visualization.chart.area.Chart();
            var model = nv.models.stackedAreaChart();
            _chart.model(model);

            model.xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });
            model.yAxis.tickFormat(function(value){
                return chart.formatValue(value);
            });
            model.clipEdge(true);
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width());
            model.yAxis.axisLabel(chart.yAxisLabel);
            if (chart.maxy !== undefined && chart.miny !== undefined) {
                model.forceY([chart.miny, chart.maxy]);
            }
            // magic to make the yaxis label show up
            // see https://github.com/novus/nvd3/issues/17
            model.margin({left: 100});
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



    $.extend(true, zenoss.visualization.chart, {
        area : area
    });

}());