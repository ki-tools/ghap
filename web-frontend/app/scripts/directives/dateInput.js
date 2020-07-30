'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:dateInput
 * @description
 * # dateInput
 */
angular.module('bmgfApp')
  .directive('dateInput', function () {
      return {
        require: 'ngModel',
        link: function(scope, element, attr, ngModelCtrl) {
          ngModelCtrl.$formatters.length = 0;
          ngModelCtrl.$parsers.length = 0;
        }
      };
  });
