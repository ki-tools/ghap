'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ModalSystemReportsCtrl
 * @description
 * # ModalSystemReportsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ModalSystemReportsCtrl', function ($scope, $modalInstance, User, Report, selectedReport) {

    $scope.error = '';
    $scope.selectedReport = selectedReport;

    User.getCurrentUser(function(user){
      $scope.user = user;
    });

    $scope.create = function create(form){
      if ((!$scope.start || !$scope.end) && selectedReport.constraintTypes.indexOf('DATE_RANGE') > -1) {
        $scope.error = 'Please select dates';
        return;
      }

      if ($scope.start > $scope.end) {
        $scope.error = 'Invalid date range';
        return;
      }

      var data = {
        start: $scope.start,
        end:   $scope.end,
      };
      $modalInstance.close(data);
    };

    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };

  });