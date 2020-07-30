'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:fileField
 * @description
 * # fileField
 */
angular.module('bmgfApp')
  .directive('fileField', function () {
    return {
      template: '<table><tbody><tr ng-repeat="file in files">' +
      '<td><input type="file" name="{{name}}" {{file.required}}></td>' +
      '<td ng-click="remove($index)" class="table-col-remove" ng-if="!file.required"></td>' +
      '<td ng-if="file.required"></td>' +
      '<td ng-click="add($index)" class="table-col-add"></td>' +
      '</tr></tbody></table>',

      scope: {
        name: '@name',
        required: '@required'
      },
      restrict: 'E',
      link: function postLink(scope) {
        scope.files = [{required:'required'}];
        scope.remove = function remove(i){
          scope.files.splice(i,1);
        };
        scope.add = function add(i){
          scope.files.splice(i+1,0, {});
        };
      }
    };
  });
