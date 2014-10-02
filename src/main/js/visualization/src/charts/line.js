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
            source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
        },

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            var _chart = chart.closure;

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                _chart.model().yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }

            chart.svg
                .datum(chart.plots)
                .transition().duration(0)
                .call(_chart.model());

            this.styleThresholds(chart.div);
        },

        build : function(chart, data) {
            var _chart = new Chart();
            var model = nv.models.lineChart();

            _chart.model(model);

            model.useInteractiveGuideline(true);

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            // ensure that there are no duplicate ticks on the y axis
            model.yAxis.tickFormat(chart.dedupeYLabels(model));
            model.yAxis.axisLabel(chart.yAxisLabel);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
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
                this.styleThresholds(chart.div);

                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                    this.styleThresholds(chart.div);
                }.bind(this));
            }.bind(this));

            // don't draw null point lines and areas
            model.lines
                .defined(function(d){
                    return d.y !== null;
                })
                .isArea(function(d) {
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

    $.extend(true, zenoss.visualization.chart, {
        line : line
    });

}());
