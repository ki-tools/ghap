'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:SubmitDatasetFilesCtrl
 * @description
 * # SubmitDatasetFilesCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('SubmitDatasetDownloadCtrl', function ($scope, DataSubmission, Settings, $interval, $location) {

    $scope.userData = [];

    var loadDataSubmissions = function() {
      if ($location.path() !== '/submit-dataset') {
        return;
      }
      DataSubmission.getData(function(data){
        $scope.userData = data;

        if(data && data.length > 200){
          $interval(loadDataSubmissions, 20000, 1);
        }
        else {
          $interval(loadDataSubmissions, 5000, 1);
        }
      });
    };

    loadDataSubmissions();

    $scope.downloadPath = function(f) {
      var accessToken = localStorage.getItem('access_token');
      return Settings.DATA_SUBMISSIONS_ROOT + '/DataSubmission/submission/'+ encodeURIComponent(f.keyName).replace(/%2F/gi, '/') + '?token=' + accessToken;
    };

    $scope.deleteFile = function(f) {
      DataSubmission.remove({action: 'delete', keyName: f.keyName}, function(){
        DataSubmission.getData(function(data){
          $scope.userData = data;
        });
      }, function(){
        $scope.error = 'Can\'t delete ' + f.keyName;
      });
    };

  });
