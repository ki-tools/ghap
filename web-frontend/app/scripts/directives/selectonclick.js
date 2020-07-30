'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:selectOnClick
 * @description
 * # selectOnClick
 */
angular.module('bmgfApp')
  .directive('selectOnClick', ['$window', function ($window) {
    return {
        restrict: 'A',
        link: function (scope, element) {
            element.on('click', function () {
                if (!$window.getSelection().toString()) {
                    // Required for mobile Safari
                    this.setSelectionRange(0, this.value.length);
                }
            });
        }
    };
}]);
