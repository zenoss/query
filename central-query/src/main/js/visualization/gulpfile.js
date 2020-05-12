var gulp = require("gulp"),
    livereload = require("gulp-livereload"),
    concat = require("gulp-concat");

var paths = {
    src: "src/",
    build: "build/",
    deploy: "../../resources/api/"
};

gulp.task("default", ["build", "copyCharts"]);

gulp.task("copyCharts", function(){
    return gulp.src([
            paths.src + "charts/line.js",
            paths.src + "charts/area.js"
        ])
        .pipe(gulp.dest(paths.deploy + "charts/"));
});

gulp.task("build", function(){

    return gulp.src([
            paths.src + "intro.js",
            paths.src + "polyfills.js",
            paths.src + "utils.js",
            paths.src + "regression.js",
            paths.src + "debug.js",
            paths.src + "dependency.js",
            paths.src + "visualization.js",
            paths.src + "LazyChartLoader.js",
            paths.src + "Chart.js",
            paths.src + "outro.js",
        ])
        .pipe(concat("visualization.js"))
        .pipe(gulp.dest(paths.build))
        .pipe(gulp.dest(paths.deploy));
        // sadly livereload doesnt work with ssl :(
        // .pipe(livereload());
});

gulp.task("watch", function(){
    gulp.watch(paths.src + "**/*", ["build", "copyCharts"]);
});
