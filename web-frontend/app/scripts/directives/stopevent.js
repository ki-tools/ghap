'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:stopEvent
 * @description
 * # stopEvent
 */
angular.module('bmgfApp')
  .directive('stopEvent', function () {
    return {
        restrict: 'A',
        link: function (scope, element) {
            element.bind('click', function (e) {
                e.stopPropagation();
            });
        }
    };
  });
