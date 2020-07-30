'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:SubmitDatasetCtrl
 * @description
 * # SubmitDatasetCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('SubmitDatasetUploadCtrl', function ($scope, $rootScope, Settings, FileUploader, User, DataSubmission) {
    $scope.user = null;
    $scope.uploaded = false;

    $rootScope.activateSessionTimeoutCheck = false;

    $scope.conflicts = [];
    $scope.uploadErrors = [];

    $scope.uploadWarning = false;

    $scope.tryToUpload = function tryToUpload(){
      if($scope.enableUploader){
        $scope.uploadWarning = false;
      }
      else {
        $scope.uploadWarning = true;
      }
    };

    $scope.$watch('enableUploader', function(){
      $scope.uploadWarning = false;
    });

    var initUploader = function initUploader(){
      // create a uploader with options
      var accessToken = localStorage.getItem('access_token');
      var uploader = $scope.uploader = new FileUploader({
          method: 'PUT',
          url: Settings.DATA_SUBMISSIONS_ROOT + '/DataSubmission/submit/',
          autoUpload: true,
          removeAfterUpload: true,
          headers: {Authorization: 'Bearer '+accessToken}
      });

      // uploader.onAfterAddingFile = function (item) {
      //     // do something
      // };
      uploader.onCompleteAll = function () {
        /*if($scope.uploadErrors.length > 0){
          console.error('Uploaded with errors', $scope.uploadErrors);
        } else {
          console.info('Complete all');
        }*/
        //TODO refresh file list
        $scope.uploaded = true;

        $rootScope.inactiveMinutes = 0;
        $rootScope.activateSessionTimeoutCheck = true;

        DataSubmission.getData();
      };
      // uploader.onSuccessItem = function (item, response, status, headers) {
      // };

      uploader.onErrorItem = function(item, response, status, headers) {// jshint ignore:line
        if (status === 409){
          $scope.conflicts.push(item);
        } else {
          $scope.uploadErrors.push({item: item, response: response, status: status});
          //console.info('onErrorItem', item, response, status, headers);
        }
        $rootScope.inactiveMinutes = 0;
        $rootScope.activateSessionTimeoutCheck = true;
      };

      // uploader.onBeforeUploadItem = function(item) {
      //     //item.formData.push({path: $scope.path});
      //     //item.url = item.url + '?path=' + $scope.path;
      // };

      uploader.onAfterAddingAll = function() {
          $scope.conflicts = [];
          $scope.uploadErrors = [];
      };

    };

    User.getCurrentUser(function getCurrentUserCallback(user){
      $scope.user = user;
      initUploader(user.guid);
    });

  });
