// Karma configuration
// http://karma-runner.github.io/0.12/config/configuration-file.html
// Generated on 2015-04-07 using
// generator-karma 0.9.0

module.exports = function(config) {
  'use strict';

  config.set({
    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,

    // base path, that will be used to resolve files and exclude
    basePath: '../',

    // testing framework to use (jasmine/mocha/qunit/...)
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
      // bower:js
      'bower_components/jquery/dist/jquery.js',
      'bower_components/es5-shim/es5-shim.js',
      'bower_components/angular/angular.js',
      'bower_components/angular-animate/angular-animate.js',
      'bower_components/angular-cookies/angular-cookies.js',
      'bower_components/angular-file-upload/angular-file-upload.js',
      'bower_components/angular-filter/dist/angular-filter.min.js',
      'bower_components/angular-resource/angular-resource.js',
      'bower_components/angular-route/angular-route.js',
      'bower_components/angular-sanitize/angular-sanitize.js',
      'bower_components/angular-touch/angular-touch.js',
      'bower_components/angular-translate/angular-translate.js',
      'bower_components/angular-ui-utils/ui-utils.js',
      'bower_components/async/lib/async.js',
      'bower_components/jquery-ui/jquery-ui.js',
      'bower_components/select2/select2.js',
      'bower_components/ua-parser-js/src/ua-parser.js',
      'bower_components/ui-select/dist/select.js',
      'bower_components/tv4/tv4.js',
      'bower_components/objectpath/lib/ObjectPath.js',
      'bower_components/angular-schema-form/dist/schema-form.js',
      'bower_components/angular-schema-form/dist/bootstrap-decorator.js',
      'bower_components/pickadate/lib/picker.js',
      'bower_components/pickadate/lib/picker.date.js',
      'bower_components/pickadate/lib/picker.time.js',
      'bower_components/angular-schema-form-datepicker/bootstrap-datepicker.min.js',
      'bower_components/angular-ui-select/dist/select.js',
      'bower_components/underscore/underscore.js',
      'bower_components/angular-schema-form-ui-select/ui-sortable.js',
      'bower_components/angular-schema-form-ui-select/angular-underscore.js',
      'bower_components/angular-schema-form-ui-select/bootstrap-ui-select.min.js',
      'bower_components/angular-mocks/angular-mocks.js',
      // endbower
      'test/set-start-url-hash.js',
      'app/scripts/**/*.js',
      'test/common.js',
      //'test/mock/**/*.js',
      'test/spec/**/*.js'
    ],

    // list of files / patterns to exclude
    exclude: [
    ],

    // web server port
    port: 8080,

    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: [
      'PhantomJS'
    ],

    // How long will Karma wait for a message from a browser before disconnecting from it (in ms).
    // default value 10000
    browserNoActivityTimeout: 20000,

    // Which plugins to enable
    plugins: [
      'karma-phantomjs-launcher',
      'karma-jasmine',
      'karma-coverage',
      'karma-junit-reporter'
    ],

    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: false,

    colors: true,

    // level of logging
    // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
    logLevel: config.LOG_INFO,

    // Uncomment the following lines if you are using grunt's server to run the tests
    // proxies: {
    //   '/': 'http://localhost:9000/'
    // },
    // URL root prevent conflicts with the site root
    // urlRoot: '_karma_',


    // coverage reports
    reporters: ['progress', 'coverage', 'junit'],

    preprocessors: {
      'app/scripts/**/*.js': ['coverage']
    },

    coverageReporter: {
      // dir : 'test/coverage/',
      reporters: [{
        type: 'html'
      }, {
        type: 'cobertura'
      },
       // {type: 'text'},
      {
        type: 'text-summary'
      }]
    }

    // junitReporter: {
    //   outputDir: 'test/results/',
    //   outputFile: 'test-results.xml'
    // }

  });
};
