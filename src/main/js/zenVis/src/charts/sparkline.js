var zenoss = zenoss || {};

// TODO - create util for this?
zenoss.viz = zenoss.viz || {};
zenoss.viz.charts = zenoss.viz.charts || {};

(function(){
	"use strict";

	var render = function(){

		var chartConfig;

		if(!this || this === window){
			throw new Error("Chart Repo methods must be bound to a Chart context");
		}

		// update existing chart
		if(this.chart){
			this.chart.load(this.model);

		// create new chart
		} else {

			var $graph = $("<div class='sparkline'><div class='thresholdIndicator'>•</div><div class='metricName'>"+ this.model.name +"</div><div class='spark' id='spark_"+ this.id +"'></div><div class='currentValue'>"+ this.config.units(this.model.columns[1][this.model.columns[1].length-1]) +"</div></div>"),
				$zenViz = this.$el.find("#zenViz_"+ this.id).append($graph);
			
			chartConfig = {
				bindto: "#spark_"+ this.id,
				data: this.model,
				point: {
					show: false
				},
				axis: {
					x: {
						show: false
					},
					y: {
						show: false
					}
				},
				legend: {
					show: false
				},
				tooltip: {
					show: false
				},
				grid: {
					y: {
						show: false,
						// TODO - per graph threshold
						lines: this.config.thresholds || []
					}
				},
				// NOTE: workaround to avoid animated data
				// interpolation. see: http://bost.ocks.org/mike/path/
				transition: {
					duration: 0
				}
			};

			this.chart = c3.generate(chartConfig);
			this.$graph = $graph;

			// evaluate thresholds
			var threshold = this.config.thresholds[0],
				// TODO - getting last element in an array shouldnt be this verbose :/
				val = this.model.columns[1][this.model.columns[1].length-1];

			if(threshold.warn.predicate(val, threshold.value)){
				this.$graph.find(".thresholdIndicator").css("color", threshold.warn.color);
			} else {
				this.$graph.find(".thresholdIndicator").css("color", "transparent");
			}
		}
	};

	// data transform for series=true
	var transformData = function(d){
		// TODO - deal with no results
		var result = d.results[0],
			config = this.seriesConfig,
			xs = {},
			columns = [],
			colors = {},
			types = {},
			names = {};

		var x = result.metric + "_x",
			y = result.metric + "_y",
			colX = [x],
			colY = [y];

		// chart config stuff
		xs[y] = x;

		// chart data
		colX = colX.concat(result.datapoints.map(function(el){return el.timestamp;}));
		colY = colY.concat(result.datapoints.map(function(el){return el.value;}));
		columns.push(colX, colY);

		this.model = {
			xs: xs,
			columns: columns,
			color: config[result.metric].color,
			type: config[result.metric].type,
			name: config[result.metric].name
		};
	};

	zenoss.viz.charts.sparkline = function(){
		this.on("update", function(data){
			transformData.call(this, data);
			render.call(this);
		}.bind(this));
	};

})();