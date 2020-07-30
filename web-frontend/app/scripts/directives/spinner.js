'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:spinner
 * @description
 * # spinner
 */
angular.module('bmgfApp')
  .directive('spinner', function () {
    return {
      template: '<div class="spinner"><div class="rect1"></div><div class="rect2"></div><div class="rect3"></div><div class="rect4"></div><div class="rect5"></div></div>',
      restrict: 'E'
    };
  });
