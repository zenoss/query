var zenoss = zenoss || {};

// TODO - create util for this?
zenoss.viz = zenoss.viz || {};
zenoss.viz.charts = zenoss.viz.charts || {};

/**
 * sparkpanel_old creates and configures many instances
 * of c3 charts as sparklines and manages them all
 *
 * sparkpanel creates many instances of the sparkline
 * chart type and updates their model, but does not
 * manage the internals
 */
(function(){
	"use strict";

	var transformData = function(d){
		var results = d.results || [],
			config = this.seriesConfig,
			firstColor;

		this.models = [];

		results.forEach(function(result){

			var columns = [],
				names = {},
				x = result.metric + "_x",
				y = result.metric + "_y",
				colX = [x],
				colY = [y];

			// sparklines should all be the same color for
			// consistancy, so use the FIRST sparkline color
			// for all
			// TODO - use a more reasonable way to pick color
			if(!firstColor) firstColor = config[result.metric].color;

			// chart config stuff
			names[y] = config[result.metric].name;

			// chart data
			colX = colX.concat(result.datapoints.map(function(el){return el.timestamp;}));
			colY = colY.concat(result.datapoints.map(function(el){return el.value;}));
			columns.push(colX, colY);

			this.models.push({
				id: result.id,
				x: x,
				columns: columns,
				color: firstColor,
				type: "line",
				names: names
			});
		}.bind(this));
	};

	var render = function(){
		var chartConfig;

		if(!this || this === window){
			throw new Error("Chart Repo methods must be bound to a Chart context");
		}

		var $zenViz = this.$el.find("#zenViz_"+ this.id);

		this.models.forEach(function(model){

			var id = model.id;

			// if this chart already exists, update it
			if(this.charts[id]){
				this.charts[id].load(model);

			// else, create a new chart
			} else {

				// TODO - make this not terrible :/
				// TODO - use more unique id
				// TODO - store each graph reference and reuse
				// on update
				var name = model.names[Object.keys(model.names)[0]],
					// TODO - use template
					$graph = $("<div class='sparkpanel'><div class='thresholdIndicator'>•</div><div class='metricName'>"+ name +"</div><div id='spark_"+ id +"' class='spark'></div>"),
					chartConfig;

				// TODO - appending junk like this causes
				// unecessary repaints/reflows
				$zenViz.append($graph);

				chartConfig = {
					bindto: "#spark_"+ id,
					data: model,
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

				// TODO - use unique id instead of model.x
				this.charts[id] = c3.generate(chartConfig);
				this.graphs[id] = $graph;
			}

			// evaluate thresholds
			// TODO - dont just use first threshold
			var threshold = this.config.thresholds[0],
				// TODO - getting last element in an array shouldnt be this verbose :/
				val = model.columns[1][model.columns[1].length-1];

			if(threshold.warn.predicate(val, threshold.value)){
				this.graphs[id].find(".thresholdIndicator").css("color", threshold.warn.color);
			} else {
				this.graphs[id].find(".thresholdIndicator").css("color", "transparent");
			}
			

		}.bind(this));
	};

	zenoss.viz.charts.sparkpanel_old = function(){
		this.charts = {};
		this.models = [];
		this.graphs = {};
		this.on("update", function(data){
			transformData.call(this, data);
			render.call(this);
		});
	};

})();