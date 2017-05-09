
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

    /**
     * When you hover over a legend reduce the opacity of the other series
     * and increase the stroke width of the hovered series to draw attention to it.
     **/
    function addHovers(chart) {
        // Change the line styles when you hover over a line on the chart.
        chart.svg.selectAll('g.nv-series').on('mouseenter', function(d){
            var key = d.key;
            chart.svg.selectAll('.nv-group').classed( {
                'zenchart_lowlight':  function(d) {
                    return (d.key !== key);
                },
                'zenchart_spotlight': function(d) {
                    return (d.key === key);
                }
            });
        });

        // Remove the hover effects when you stop hovering over a series.
        chart.svg.selectAll('g.nv-series').on('mouseleave', function() {
            chart.svg.selectAll('.nv-group').classed({'zenchart_lowlight': false, 'zenchart_spotlight': false});
        });

    }

    var line = {
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
            var _chart = chart.closure,
                model = _chart.model();

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, model.xAxis);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }

            // if we have a legendState stored,
            // use it to mark if a plot is disabled
            // so that graph will preserve series
            // toggle state
            if(model.legendState){
                chart.plots.forEach(function(plot, i){
                    plot.disabled = model.legendState[i];
                });
            }

            chart.svg
                .datum(chart.plots)
                .transition().duration(0)
                .call(_chart.model());

            this.styleThresholds(chart.$div);
            this.styleProjections(chart);
            addHovers(chart);
        },

        build : function(chart, data) {
            var _chart = new Chart();
            var model = nv.models.lineChart();

            _chart.model(model);

            model.useInteractiveGuideline(true);
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

            // if this is intended to be printed, ain't no
            // reason to show the legend!
            if(chart.printOptimized){
                model.showLegend(false);
            }

            // ensure that there are no duplicate ticks on the y axis
            model.yAxis.tickFormat(chart.dedupeYLabels(model));
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
            model.margin({
                left : 90
            });

            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width() - 10);

            chart.svg.datum(chart.plots);

            nv.addGraph(function() {
                chart.svg.transition().duration(0).call(model);
                this.styleThresholds(chart.$div);
                this.styleProjections(chart);
                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                    this.styleThresholds(chart.$div);
                    this.styleProjections(chart);
                }.bind(this));
                addHovers(chart);
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
            model.width($(chart.svgwrapper).width());
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
        },
        /**
         * All plot series that have the "projection" attribute to true will
         * be styled distinctively
         **/
        styleProjections: function(chart) {
            var i, n;
            for (i = 0; i < chart.plots.length; i ++ ) {
                if (chart.plots[i].projection) {
                    n = i - 1;
                    d3.select('#' + chart.name).selectAll('.nv-series-' +
                                                n.toString()).selectAll('.nv-line').style("stroke-dasharray", ('3, 3'));

                }
            }
        }
    };

    $.extend(true, zenoss.visualization.chart, {
        line : line
    });

}());
