'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:selectContent
 * @description
 * # selectContent
 */
angular.module('bmgfApp')
  .directive('selectContent', function ($timeout) {
    return {
      restrict: 'A',
      link: function postLink(scope, element) {
        var select = function(){
          element[0].setSelectionRange(0, element[0].value.length);
        };
        $timeout(select, 100);
      }
    };
  });
