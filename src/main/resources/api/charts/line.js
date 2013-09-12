(function() {
    "use strict";
    var line = {
        required : {
            defined : 'nv',
            source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
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

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            var _chart = chart.closure;
            _chart.model().xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts);
            });

            chart.svg.datum(chart.plots).transition().duration(0).call(
                    _chart.model());
        },

        build : function(chart, data) {
            var _chart = new zenoss.visualization.chart.line.Chart();
            var model = nv.models.lineChart();
            _chart.model(model);
	 
            model.xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts);
            });
            model.yAxis.tickFormat(function(value){
                return chart.formatValue(value);
            });
            model.yAxis.axisLabel(chart.yAxisLabel);

            if (chart.maxy !== undefined && chart.miny !== undefined) {
                model.forceY([chart.miny, chart.maxy]);
            }
            // magic to make the yaxis label show up
            // see https://github.com/novus/nvd3/issues/17
            model.margin({left: 100});
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width());
            chart.svg.datum(chart.plots);
            nv.addGraph(function() {
                chart.svg.transition().duration(0).call(model);
                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                });
            });

            return _chart;
        },

        render : function() {
        }
    };
    $.extend(true, zenoss.visualization.chart, {
        line : line
    });
}());