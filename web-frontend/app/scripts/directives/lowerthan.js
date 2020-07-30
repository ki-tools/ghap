'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:lowerThan
 * @description
 * # lowerThan
 */
angular.module('bmgfApp')
  .directive('lowerThan', [
  function() {

    var link = function($scope, $element, $attrs, ctrl) {

      var validate = function(viewValue, useRawValue) {
        var comparisonModel = $attrs.lowerThan;

        if(!viewValue || !comparisonModel){
          // It's valid because we have nothing to compare against
          ctrl.$setValidity('lowerThan', true);
          return viewValue;
        }

        var parsedVal = viewValue.indexOf('-') > 0 ? Date.parse(viewValue + (useRawValue ? '':'T00:00:00.000Z')):parseInt(viewValue, 10);
        var parsedComp = comparisonModel.indexOf('-') > 0 ?       Date.parse(comparisonModel):parseInt(comparisonModel, 10);

        // It's valid if model is lower than the model we're comparing against
        ctrl.$setValidity('lowerThan', parsedVal < parsedComp );
        return viewValue;
      };

      ctrl.$parsers.unshift(validate);
      ctrl.$formatters.push(validate);

      $attrs.$observe('lowerThan', function(comparisonModel){
        //jshint unused:false
        return validate($scope.compareWith ? $scope.compareWith:ctrl.$viewValue, $scope.compareWith);
      });

      $attrs.$observe('compareWith', function(comparisonModel){
        //jshint unused:false
        return validate($scope.compareWith ? $scope.compareWith:ctrl.$viewValue, $scope.compareWith);
      });

    };

    return {
      require: 'ngModel',
      scope: {
        compareWith: '@'
      },
      link: link
    };

  }
]);
