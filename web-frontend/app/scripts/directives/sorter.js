'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:sorter
 * @description
 * # sorter
 */
angular.module('bmgfApp')
  .directive('sorter', function(){
    return {
        restrict: 'AE',
        transclude: true,
        scope: {
            predicate: '=',
            reverse: '=',
            attr: '@'
        },
        template: '<div ng-click="sort($event)"><span ng-transclude/> <i class="caret small" ng-hide="reverse || predicate != attr">&#9650;</i><i  class="caret small" ng-hide="!reverse || predicate != attr">&#9660;</i></div>',
        link: function (scope) {
            scope.sort = function ($event) {
                $event.preventDefault();
                scope.predicate = (scope.attr.indexOf(',') >= 0) ? scope.attr.split(',') : scope.attr;
                scope.reverse = !scope.reverse;
            };
        }
    };
});
