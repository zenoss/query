var gulp = require("gulp"),
	browserify = require("gulp-browserify"),
	rename = require("gulp-rename"),
	livereload = require("gulp-livereload");

var paths = {
	src: "src/**/*",
	build: "build/",
	deploy: "/home/john/src/europa/src/golang/src/github.com/zenoss/serviced/web/static/js/"
};

gulp.task("default", ["build", "watch"]);

gulp.task("build", function(){

	var browserifyConfig = {
		debug: true
		// shim: {
		// 	nvd3: {
		// 		path: "./lib/nv.d3.js",
		// 		exports: "nv",
		// 		depends: {
		// 			d3: "d3"
		// 		}
		// 	},
		// 	d3: {
		// 		path: "./lib/d3.js",
		// 		exports: "d3"
		// 	}
		// }
	};

	return gulp.src(paths.src + "app.js", {read: false})
		.pipe(browserify(browserifyConfig))
		.pipe(rename("visualization.js"))
		.pipe(gulp.dest(paths.build))
		.pipe(gulp.dest(paths.deploy))
		// sadly livereload doesnt work with ssl :(
		.pipe(livereload());
});

gulp.task("watch", function(){
	gulp.watch(paths.src, ["build"]);
});