'use strict';

/*jshint multistr: true */

/**
 * @ngdoc directive
 * @name bmgfApp.directive:filter
 * @description
 * # filter
 */
angular.module('bmgfApp')
  .directive('filter', function () {
    return {
      templateUrl: 'views/directives/filter.html',
      scope: {
        field: '=',
        placeholder: '@'
      },
      link: function (scope) {
        scope.clearFilter = function() {
          scope.field = '';
        };
      }
    };
  });
