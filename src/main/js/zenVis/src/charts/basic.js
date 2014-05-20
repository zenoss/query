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

			var $zenViz = this.$el.find("#zenViz_"+ this.id).append("<div class='basic' id='basic_"+ this.id +"'>");
			
			chartConfig = {
				bindto: '#basic_'+ this.id,
				data: this.model,
				point: {
					show: false
				},
				axis: {
					x: {
						type: "timeseries",
						tick: {
							format: function(x){
								return moment(x).subtract("minutes", zenoss.utils.tzOffset()).format("h:mm:ssa");
							}
						}
					},
					y: {
						tick: {
							format: this.config.units
						}
					}
				},
				grid: {
					y: {
						show: true,
						lines: this.config.thresholds || []
					}
				},
				legend: {
					offset: {
						alignY: "top",
						alignX: "right",
						x: -5,
						y: 5
					}
				},
				// NOTE: workaround to avoid animated data
				// interpolation. see: http://bost.ocks.org/mike/path/
				transition: {
					duration: 0
				},
				// subchart: {
				// 	show: true,
				// 	hideAxis: true,
				// 	size: {
				// 		height: 30
				// 	}
				// }
				// zoom: {
				// 	enabled: true
				// }
				// TODO - thresholds using regions
			};

			this.chart = c3.generate(chartConfig);

			// create new controls
			renderControls.call(this);
		}
	};

	var renderControls = function(){
		// TODO - bind to template
		// TODO - 2 way data bind?
		// TODO - cache selectors
		if(this.config.autoUpdate){
			this.$el.append("<div class='controls'><span class='autoUpdate'><input type='checkbox' class='autoUpdateCheck'>auto-update</span></div>");
			if(this.config.autoUpdate) this.$el.find(".autoUpdateCheck").prop("checked", true);

			// events hash?
			this.$el.on("click", ".autoUpdate", function(e){

				// if already checked
				if(this._shouldAutoUpdate){
					this.stopAutoUpdate();
				} else {
					this.startAutoUpdate();
				}

				$(e.target).find(".autoUpdateCheck").prop("checked", this._shouldAutoUpdate);

			}.bind(this));
		}

	};

	// data transform for series=false
	/*var transformData = function(d){
		var results = d.results || [],
			config = this.seriesConfig,
			columnLookup = {},
			xs = {},
			columns = [],
			colors = {},
			types = {},
			names = {};

		results.forEach(function(result){

			var x = result.metric + "_x",
				y = result.metric + "_y";

			// TODO - factor this into its own
			// "add series" type function
			if(!columnLookup[x] || !columnLookup[y]){
				columnLookup[x] = [x];
				columnLookup[y] = [y];
				columns.push(columnLookup[x]);
				columns.push(columnLookup[y]);
				xs[y] = x;
				colors[y] = config[result.metric].color;
				types[y] = config[result.metric].fill ? "area" : "line";
				names[y] = config[result.metric].key;
			}

			columnLookup[x].push(result.timestamp);
			columnLookup[y].push(result.value);
		});

		this.model = {
			xs: xs,
			columns: columns,
			colors: colors,
			types: types,
			names: names
		};
	};*/
	// data transform for series=true
	var transformData = function(d){
		var results = d.results || [],
			config = this.seriesConfig,
			xs = {},
			columns = [],
			colors = {},
			types = {},
			names = {};

		results.forEach(function(result){

			var x = result.metric + "_x",
				y = result.metric + "_y",
				colX = [x],
				colY = [y];

			// chart config stuff
			xs[y] = x;
			colors[y] = config[result.metric].color;
			types[y] = config[result.metric].type;
			names[y] = config[result.metric].name;

			// chart data
			colX = colX.concat(result.datapoints.map(function(el){return el.timestamp;}));
			colY = colY.concat(result.datapoints.map(function(el){return el.value;}));
			columns.push(colX, colY);
		});

		this.model = {
			xs: xs,
			columns: columns,
			colors: colors,
			types: types,
			names: names
		};

		console.log(this.model);
	};

	zenoss.viz.charts.basic = function(){
		this.on("update", function(data){
			transformData.call(this, data);
			render.call(this);
		}.bind(this));
	};

})();