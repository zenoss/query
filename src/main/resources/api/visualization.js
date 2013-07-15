var zenoss = {
	visualization : {
		debug : false,
		url : "http://localhost:8080",
		defaults : {
			range : {
				start : "1h-aog",
				end : "now"
			},
			filter : {

			},
			aggregation : "avg"
		},

		load : function(callback) {
			function loadScript(url, async, callback) {
				var script = document.createElement("script")
				script.type = "text/javascript";
				if (async) {
					script.async = true;
				}

				if (script.readyState) { // IE
					script.onreadystatechange = function() {
						if (script.readyState == "loaded"
								|| script.readyState == "complete") {
							script.onreadystatechange = null;
							callback();
						}
					};
				} else { // Others
					script.onload = function() {
						callback();
					};
				}

				script.src = url;
				document.getElementsByTagName("head")[0].appendChild(script);
			}

			function includeCSS(url) {
				var css = document.createElement('link');
				css.rel = 'stylesheet'
				css.type = 'text/css';
				css.href = url;
				document.getElementsByTagName('head')[0].appendChild(css);
			}

			// Script order matters and as this is all loaded asynchronously the
			// loads are nested. Bootstrap load JQuery and then use JQuery to
			// load everything else
			loadScript(
					'http://localhost:8888/api/jquery.min.js',
					true,
					function() {
						if (zenoss.visualization.debug) {
							console.log('JQuery loaded');
						}
						$
								.getScript(
										'http://localhost:8888/api/d3.v3.min.js',
										function() {
											if (zenoss.visualization.debug) {
												console.log('D3 loaded');
											}
											$
													.getScript(
															'http://localhost:8888/api/nv.d3.min.js',
															function() {
																if (zenoss.visualization.debug) {
																	console
																			.log('NV.D3 Loaded');
																}
																includeCSS('http://localhost:8888/api/css/nv.d3.css');
																if (zenoss.visualization.debug) {
																	console
																			.log("Loaded NV.D3 CSS");
																}
																includeCSS('http://localhost:8888/api/css/zenoss.css');
																if (zenoss.visualization.debug) {
																	console
																			.log("Loaded Zenoss CSS");
																}
																callback();
															});
										});
					});
		},
		timerange : {
			create : function(name, startTime, endTime) {
				return new zenoss.visualization.TimeRange(name, startTime,
						endTime);
			}
		},
		TimeRange : function(name, startTime, endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
			var _name = name;
			this.name = function() {
				return _name;
			}
			this.type = function() {
				return "TimeRange";
			}
			this.range = function(startTime, endTime) {
				this.startTime = startTime;
				this.endTime = endTime;
			}
		},
		queryfilter : {
			create : function(name) {
				return new zenoss.visualization.QueryFilter(name);
			}
		},
		QueryFilter : function(name) {
			var _name = name;
			this.name = function() {
				return _name;
			}
			this.type = function() {
				return "QueryFilter";
			}
			this.set = function(name, value) {

			}
			this.get = function(name) {

			}
			this.toFilterString = function() {

			}
		},
		dataset : {
			create : function(name) {
				return new zenoss.visualization.Dataset(name);
			}
		},
		Dataset : function(name) {
			var _name = name;
			this.name = function() {
				return _name;
			}
			this.type = function() {
				return "Dataset";
			}
			this.range = function(p1, p2) {

			}
			this.queryFilter = function(p1) {

			}
			this.query = function(q) {

			}
			this.load = function() {

			}
		},
		plot : {
			create : function(name) {
				return new zenoss.visualization.Plot(name);
			}
		},
		Plot : function(name, options) {
			var _name = name;
			var _options = options;
			var _dataset = null;
			var _range = null;
			var _query = null;

			this.name = function() {
				return _name;
			}
			this.type = function() {
				return "Plot";
			}
			this.dataset = function(ds) {
				_dataset = ds;
			}
			this.range = function(p1, p2) {

			}
			this.query = function(query) {

			}
			this.include = function(name, options) {

			}
			this.add = function(name, type, options) {

			}
		},
		chart : {
			create : function(name, arg1, arg2) {

				function showError(name, err, detail) {
					$('#' + name).html(
							'<span class="zenerror">' + detail + '</span>');
				}

				function loadChart(name, callback, onerror) {
					$
							.ajax({
								'url' : zenoss.visualization.url
										+ '/chart/name/' + name,
								'type' : 'GET',
								'dataType' : 'json',
								'contentType' : 'application/json',
								'success' : function(data) {
									callback(data);
								},
								'error' : function(response) {
									var err = JSON.parse(response.responseText);
									var detail = 'Error while attempting to fetch chart resource with the name "'
											+ name
											+ '", via the URL "'
											+ zenoss.visualization.url
											+ '/chart/name/'
											+ name
											+ '", the reported error was "'
											+ err.errorSource
											+ ':'
											+ err.errorMessage + '"';
									if (typeof onerror != 'undefined') {
										onerror(err, detail);
									}
								}
							});
				}

				if (typeof arg1 == 'string') {
					// A chart template name was specified, so we need to first
					// load that template and then create the chart based on
					// that.
					var config = arg2;
					if (typeof jQuery == 'undefined') {
						loadRequiredLibraries(function() {
							loadChart(arg1, function(template) {
								return new zenoss.visualization.Chart(name,
										template, config);
							}, function(err, detail) {
								showError(name, err, detail);
							});
						});
						return;
					}
					loadChart(arg1, function(template) {
						return new zenoss.visualization.Chart(name, template,
								config);
					}, function(err, detail) {
						showError(name, err, detail);
					});
					return;
				}

				var template = null;
				var config = arg1;

				if (typeof jQuery == 'undefined') {
					loadRequiredLibraries(function() {
						return new zenoss.visualization.Chart(name, template,
								config);
					});
					return;
				}
				zenoss.visualization.Chart(name, template, config);
			}
		},

		/**
		 * Constructs and renders a chart / graph in the "div" elements
		 * specified by the "name" parameter.
		 * 
		 * @param name
		 *            specifies the "div" element in the HTML document that will
		 *            be replaced by the chart.
		 * @param template
		 *            specifies the template chart, this is the chart that was
		 *            fetched from the metric service
		 * @param config
		 *            if not template is specified this contains the chart
		 *            specification, if a template is specified this contains
		 *            "override" values for the template.
		 */
		Chart : function(name, template, config) {

			function showError(name, err, detail) {
				$('#' + name).html(
						'<span class="zenerror">Error: ' + detail + '</span>');
			}

			function processResultAsSeries(request, data) {
				var plots = [];

				data.results.forEach(function(result) {
					// The key for a series plot will be its distinguishing
					// characteristics, which is the metric name and the
					// tags / filter
					var key = result.metric;
					if (typeof result.tags != 'undefined') {
						key += '{';
						var prefix = '';
						for ( var tag in result.tags) {
							if (result.tags.hasOwnProperty(tag)) {
								key += prefix + tag + '=' + result.tags[tag];
								prefix = ',';
							}
						}
						key += '};'
					}
					var plot = {
						'key' : key,
						'values' : []
					};
					result.datapoints.forEach(function(dp) {
						plot.values.push({
							'x' : dp.timestamp * 1000,
							'y' : dp.value
						});
					});
					plots.push(plot);
				});

				return plots;
			}

			function processResultAsDefault(request, data) {

				var plotMap = Array();

				// Create a plot for each metric name, this is essentially
				// grouping the results by metric name. This can cause problems
				// if the request contains multiple queries for the same
				// metric, but this is basically a restriction of the
				// implementation (OpenTSDB) where it doesn't split the results
				// logically when multiple requests are made in a single call.
				data.results.forEach(function(result) {
					var plot = plotMap[result.metric];
					if (typeof plot == 'undefined') {
						plot = {
							'key' : result.metric,
							'values' : []
						};
						plotMap[result.metric] = plot;
					}

					plot.values.push({
						'x' : result.timestamp * 1000,
						'y' : result.value
					});
				});

				// Convert the plotMap into an array of plots for the graph
				// library to process
				var plots = [];
				for ( var key in plotMap) {
					if (plotMap.hasOwnProperty(key)) {
						// Sort the values of the plot as we put them in the
						// plots aray.
						plotMap[key].values.sort(function compare(a, b) {
							if (a.x < b.x)
								return -1;
							if (a.x > b.x)
								return 1;
							return 0;
						});
						plots.push(plotMap[key]);
					}
				}
				return plots;
			}

			function processResult(request, data) {
				if (data.series) {
					return processResultAsSeries(request, data);
				}
				return processResultAsDefault(request, data);
			}

			/**
			 * Deep object merge. This merge differs significantly from the
			 * "extend" method provide by jQuery in that it will merge the value
			 * of arrays, but concatenating the arrays together using the jQuery
			 * method "merge". Neither of the objects passed are modified and a
			 * new object is returned.
			 * 
			 * @param a
			 *            object 1
			 * @param b
			 *            object 2
			 * @returns the merged object
			 */
			function merge(a, b) {
				if (typeof a == 'undefined' || a == null) {
					return $.extend(true, {}, b);
				}
				if (typeof b == 'undefined' || b == null) {
					return $.extend(true, {}, a);
				}

				var m = $.extend(true, {}, a);

				for ( var k in b) {
					if (b.hasOwnProperty(k)) {
						var v = b[k];
						if (v.constructor == Number || v.constructor == String) {
							m[k] = v;
						} else if (v instanceof Array) {
							m[k] = $.merge(m[k], v);
						} else if (v instanceof Object) {
							if (typeof m[k] == 'undefined') {
								m[k] = $.extend({}, v);
							} else {
								m[k] = merge(m[k], v);
							}
						} else {
							m[k] = $.extend(m[k], v);
						}
					}
				}

				return m;
			}

			// By this point all the supporting libraries should be loaded,
			// if they are not then we can not continue. Verify this with
			// a simple check for the NVD3 library.
			if (typeof nv == 'undefined') {
				console
						.error("Error: not all supporting libraries were loaded, cannot continue");
				alert("Error: not all supporting libraries were loaded, cannot continue");
				return;
			}

			var _name = name;
			var _config = merge(template, config);

			this.name = function() {
				return _name;
			}
			this.type = function() {
				return "Chart";
			}
			this.add = function(name, options) {
				return zenoss.visualization.plot.create(name, options);
			}

			var div = $('#' + name);
			if (typeof _config != 'undefined') {
				if (typeof _config.width != 'undefined') {
					div.width(_config.width);
				}
				if (typeof _config.height != 'undefined') {
					div.height(_config.height);
				}
			}
			// div.css('background-color', 'red');
			var timeFormat = function(ts) {
				return d3.time.format('%x %X')(new Date(ts));
			}

			var request = {};
			if (typeof _config != 'undefined') {
				if (typeof _config.range != 'undefined') {
					if (typeof _config.range.start != 'undefined') {
						request.start = _config.range.start;
					}
					if (typeof _config.range.end != 'undefined') {
						request.end = _config.range.end;
					}
				}

				if (typeof _config.series != 'undefined') {
					request.series = _config.series;
				}

				if (typeof _config.datapoints != 'undefined') {
					request.metrics = [];
					_config.datapoints.forEach(function(dp) {
						var m = {};
						m.metric = dp.metric;
						if (typeof dp.rate != 'undefined') {
							m.rate = dp.rate;
						}
						if (typeof dp.aggregator != 'undefined') {
							m.aggregator = dp.aggregator;
						}
						if (typeof dp.downsample != 'undefined') {
							m.downsample = dp.downsample;
						}
						if (typeof dp.filter != 'undefined') {
							m.tags = {};
							for ( var key in dp.filter) {
								if (dp.filter.hasOwnProperty(key)) {
									m.tags[key] = dp.filter[key];
								}
							}
						}
						request.metrics.push(m);
					});
				}
			}

			if (typeof request.metrics == 'undefined') {
				return;
			}

			if (zenoss.visualization.debug) {
				console.log('POST: ' + zenoss.visualization.url
						+ '/query/performance: ' + JSON.stringify(request));
			}
			$
					.ajax({
						'url' : zenoss.visualization.url + '/query/performance',
						'type' : 'POST',
						'data' : JSON.stringify(request),
						'dataType' : 'json',
						'contentType' : 'application/json',
						'success' : function(data) {
							var plots = processResult(request, data);

							var type = "line";
							if (typeof _config.type != 'undefined') {
								type = _config.type;
							}

							var _chart = null;
							switch (type) {
							case "area":
								// Area plots don't seem to do well if there are
								// multiple data point sets and there are not
								// the same
								// number of points in each set, so tuncate the
								// data
								// point areas to the same number of points.
								if (plots.length > 1) {
									// get minmum length
									var minLength = plots[0].values.length;
									plots.forEach(function(plot) {
										minLength = Math.min(minLength,
												plot.values.length);
									});

									// Truncate
									plots.forEach(function(plot) {
										plot.values.length = minLength;
									});

								}
								_chart = nv.models.stackedAreaChart().x(
										function(v) {
											return v.x;
										}).y(function(v) {
									return v.y;
								}).clipEdge(true);
								break;
							case "bar":
								_chart = nv.models.multiBarChart();
								break;
							case "focus":
								_chart = nv.models.lineWithFocusChart();
								break;
							case "line":
							default:
								_chart = nv.models.lineChart();
								break;
							}
							var _svg = null;

							nv.addGraph(function() {
								_chart.xAxis.tickFormat(timeFormat).axisLabel(
										'Date/Time');
								_chart.yAxis.axisLabel('Memory');
								_svg = d3.select('#' + _name).append('svg');
								_svg.datum(plots).transition().duration(500)
										.call(_chart);
								nv.utils.windowResize(function() {
									_svg.call(_chart)
								});
							});
						},
						'error' : function(res) {
							// Many, many reasons that we might have gotten
							// here, with
							// most of them we are not able to detect why. If we
							// have
							// a readystate of 4 and an response code in the
							// 200s that
							// likely means we were unable to parse the JSON
							// returned
							// from the server. If not that then who knows ....
							if (res.readyState == 4
									&& Math.floor(res.status / 100) == 2) {
								var detail = 'Severe: Unable to parse data returned from Zenoss metric service as JSON object. Please copy / paste the REQUEST and RESPONSE written to your browser\'s Java Console into an email to Zenoss Support';
								console.group('Severe error, please report');
								console
										.error('REQUEST : POST /query/performance: '
												+ JSON.stringify(request));
								console.error('RESPONSE: ' + res.responseText);
								console.groupEnd();
								showError(_name, {}, detail);
							} else {
								var err = JSON.parse(res.responseText);
								var detail = 'An unexpected failure response was received from the server. The reported message is: '
										+ err.errorSource
										+ ' : '
										+ err.errorMessage;
								console.error(detail);
								showError(_name, err, detail);
							}
						}
					});
		}
	}
}