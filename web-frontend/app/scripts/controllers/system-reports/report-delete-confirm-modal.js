'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:UsageReportsSystemReportsCtrl
 * @description
 * # UsageReportsSystemReportsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ReportDeleteConfirmModalCtrl', function ($scope, $modalInstance) {

    $scope.del = function(){
      $modalInstance.close();
    };

    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
    
  })