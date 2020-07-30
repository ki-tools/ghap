'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:MyAccountCtrl
 * @description
 * # MyAccountCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('MyAccountCtrl', function ($scope, Settings, User, $location, ErrorTranslator) {

    $scope.user = [];
    $scope.errors = [];

    $scope.activities = [];

    User.getCurrentUser(
      function(user){
        $scope.user = user;
        if( User.isResetPassword() ){
          $location.path('/password-reset');
        } else {
          User.getActivities(function(activities){
            $scope.activities = activities;
          });
        }
      },
      function(){
        User.dropCurrentUser(function(){
          document.location.href = Settings.OAUTH_URL + '/oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=' + document.location.href;
        });
      }
    );

    $scope.update = function() {
      $scope.success = '';
      $scope.errors = [];

      if ($scope.user.password) {
        if ($scope.user.password !== $scope.passwordConfirmation) {
          $scope.errors.push('Invalid password confirmation.');
          return;
        }
        $scope.passwordConfirmation = '';
      }

      $scope.user.notifyByEmail = true;
      User.save($scope.user, function(user) {
        $scope.success = 'Account successfully updated';
      	User.setCurrentUser($scope.user = user);
      }, function(httpResponse) {
        var errors = (httpResponse.data && httpResponse.data.errors) ? httpResponse.data.errors : [];

        if(httpResponse.status === 400 && httpResponse.data && errors.length > 0){
          $scope.user = angular.copy(httpResponse.data);
          delete $scope.user.errors;
          User.setCurrentUser($scope.user); // user can be updated except "password" field
        }

        $scope.errors.push.apply($scope.errors, ErrorTranslator.populate(errors));
      });
    };

  });
