'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:UserManagementCreateAccountCtrl
 * @description
 * # UserManagementCreateAccountCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('UserManagementCreateAccountCtrl', function ($scope, User, $location, $rootScope, PersonalStorage, ErrorTranslator) {

    $scope.user = {};

    $scope.errors = [];

    $scope.reset = function() {
      $scope.user = {
        name: '',
        firstName: '',
        lastName: '',
        email: '',
        generatePassword: false,
        password: '',
        notifyByEmail: true
      };
    };

    $scope.submit = function() {
      $scope.success = '';
      $scope.errors = [];

      if($scope.user.generatePassword){
        $scope.user.password = '';
      }

      User.save($scope.user, function(data) {
        $scope.success = 'User successfully created';
        if ($scope.user.storage) {
          PersonalStorage.create({guid: data.guid, size: 500}, function(data){
            $scope.success += ', personal storage created'
            $scope.reset();
          }, function(httpResponse) {
            if (!httpResponse.data) {
              $scope.errors.push('Error creating personal storage');
            }

            var errors = httpResponse.data.errors;
            for (var i = 0; i < errors.length; i++) {
              $scope.errors.push(errors[0].field + ': ' + errors[0].errors[0].message);
            }
          });
        }
      }, function(httpResponse) {
        if (!httpResponse.data) {
          $scope.errors.push('Error creating user');
        }

        var errors = (httpResponse.data && httpResponse.data.errors) ? httpResponse.data.errors : [];
        $scope.errors.push.apply($scope.errors, ErrorTranslator.populate(errors));
      });
    };

    $scope.reset();

  });
