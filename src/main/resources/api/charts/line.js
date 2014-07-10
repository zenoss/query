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

    var line = {
        required : {
            defined : 'nv',
            source : [ 'nv.d3.js', 'css/nv.d3.css' ]
        },

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            var _chart = chart.closure;

            _chart.model().xAxis.tickFormat(function(ts) {
                return chart.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });
            chart.svg.datum(chart.plots).transition().duration(0).call(
                    _chart.model());
        },

        build : function(chart, data) {
            var _chart = new Chart();
            var model = nv.models.lineChart();
            _chart.model(model);

            model.xAxis.tickFormat(function(ts) {
                return chart.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });
            model.yAxis.tickFormat(function(value) {
                return chart.formatValue(value);
            });
            model.yAxis.axisLabel(chart.yAxisLabel);

            if (chart.maxy !== undefined && chart.miny !== undefined) {
                model.forceY([ chart.miny, chart.maxy ]);
            }
            // magic to make the yaxis label show up
            // see https://github.com/novus/nvd3/issues/17
            model.margin({
                left : 90
            });
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width() - 10);
            chart.svg.datum(chart.plots);
            nv.addGraph(function() {
                chart.svg.transition().duration(0).call(model);
                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                });
            });

            model.lines.isArea(function(d) {
                return d.fill;
            });
            return _chart;
        },

        resize : function(chart) {
            var _chart = chart.closure, model = _chart.model();
            model.height($(chart.svgwrapper).height());
            chart.svg.transition().duration(0).call(model);
        },

        render : function() {
        }
    };
    $.extend(true, zenoss.visualization.chart, {
        line : line
    });
}());