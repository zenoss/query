(function(){
	"use strict";

	// assume jquery
	$(document).ready(function(){

		var $configBox = $("#configBox");
		$configBox.on("keyup", function(e){
			var configTxt = $configBox.val(),
				configObj;

			try {
				$("#chartyChart").html("");
				eval("configObj="+configTxt);
			} catch(err){
				return;
			}

			new zenoss.ZenVis(configObj);
		});
		$configBox.trigger("keyup");


		// new zenoss.ZenVis({
		// 	// configures chart in the UI
		// 	$el: $("#chartyChart"),
		// 	units: zenoss.ZenVis.prototype.units.PERCENT,
		// 	autoUpdate: 10000,
		// 	start: "30m-ago",
		// 	end: "now",
		// 	series: [
		// 		{
		// 			metric: "proc.loadavg.15min"
		// 		},{
		// 			metric: "proc.loadavg.5min"
		// 		},{
		// 			name: "Load Avg, 1min",
		// 			color: "green",
		// 			metric: "proc.loadavg.1min",
		// 			type: "area"
		// 		}
		// 	],
		// 	thresholds: [
		// 		{
		// 			color: "#FF0000",
		// 			value: 0.2,
		// 			text: ""
		// 		}
		// 	]
		// });
		// new zenoss.ZenVis({
		// 	// configures chart in the UI
		// 	$el: $("#chartyChart"),
		// 	units: zenoss.ZenVis.prototype.units.PERCENT,
		// 	type: "sparkpanel2",
		// 	autoUpdate: 10000,
		// 	start: "10m-ago",
		// 	end: "now",
		// 	series: [
		// 		{
		// 			name: "CPU% 15min",
		// 			metric: "proc.loadavg.15min",
		// 			downsample : "1m-avg"
		// 		},{
		// 			name: "CPU% 5min",
		// 			metric: "proc.loadavg.5min",
		// 			downsample : "1m-avg"
		// 		},{
		// 			name: "CPU% 1min",
		// 			metric: "proc.loadavg.1min",
		// 			downsample : "1m-avg"
		// 		},{
		// 			name: "Mem Free",
		// 			metric: "proc.meminfo.memfree",
		// 			downsample : "1m-avg"
		// 		},{
		// 			name: "Mem Free",
		// 			metric: "proc.meminfo.memfree",
		// 			downsample : "1m-avg"
		// 		},{
		// 			name: "Mem Active",
		// 			metric: "proc.meminfo.active",
		// 			downsample : "1m-avg"
		// 		}
		//     ],
		// 	thresholds: [
		// 		{
		// 			color: "#A83939",
		// 			value: 0.5,
		// 			warn: {
		// 				predicate: function(a,b){return a>b;},
		// 				color: "#A83939"
		// 			}
		// 		}
		// 	]
		// });
		// new zenoss.ZenVis({
		// 	// configures chart in the UI
		// 	$el: $("#chartyChart"),
		// 	type: "sparkline",
		// 	autoUpdate: 1000,
		// 	start: "10m-ago",
		// 	end: "now",
		// 	series: [
		// 		{
		// 			name: "CPU% 15min",
		// 			metric: "proc.loadavg.15min",
		// 			downsample : "1m-avg"
		// 		}
		//     ],
		// 	thresholds: [
		// 		{
		// 			color: "rgba(168,57,57,.2)",
		// 			value: 0.2,
		// 			warn: {
		// 				predicate: function(a,b){return a>b;},
		// 				color: "#A83939"
		// 			}
		// 		}
		// 	]
		// });


	});


})();