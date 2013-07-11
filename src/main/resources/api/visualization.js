var zenoss = {
	visualization : {
		url : "http://localhost:80808",
		defaults : {
			range : {
				start : "1h-aog",
				end : "now"
			},
			filter : {

			},
			aggregation : "avg"
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
			create : function(name, config) {
				return new zenoss.visualization.Chart(name);
			}
		},
		Chart : function(name) {
			var _name = name;
			this.name = function() {
				return _name;
			}
			this.type = function() {
				return "Chart";
			}
			this.add = function(name, options) {
				return zenoss.visualization.plot.create(name, options);
			}
		}
	}
}