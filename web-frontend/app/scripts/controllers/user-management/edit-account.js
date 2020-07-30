'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:UserManagementEditAccountCtrl
 * @description
 * # UserManagementEditAccountCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
.controller('UserManagementEditAccountCtrl', function ($scope, User, PersonalStorage, $routeParams, $location, $timeout, $modal, Settings, $filter) {

  // $scope.USERS_URL = Settings.API_ROOT + '/user/all/default';

  $scope.userFilter = {name: ''};

  $scope.selectedUser = {};
  $scope.selectedUserIdx = -1;

  $scope.currentUser = null;
  $scope.users = [];

  $scope.errors = [];

  $scope.delete = deleteUser;
  $scope.get = get;
  $scope.changeStorage = changeStorage;
  $scope.update = update;
  $scope.go = go;

  activate();

  function activate(){
    User.getCurrentUser(function(currentUser){
      $scope.currentUser = currentUser;
    });

    User.list(function(users) {
      $scope.errors = [];
      $scope.users = users;
      var userDn = users[0].dn || $routeParams.dn;
      $routeParams.dn = users[0].dn;
      $scope.get({dn: userDn}, 0);
    }, function(httpResponse) {
      $scope.errors.push(httpResponse.data);
    });
  }

  function go(path) {
    $location.path(path);
  };

  function get(user, idx) {
    //$scope.go('/user-management/edit-account/' + user.dn);
    $scope.selectedUserIdx = idx;
    $scope.errors = [];

    $scope.selectedUser = user;
    $scope.selectedUser.notifyByEmail = true;
    $scope.selectedUser.disabled = user.disabled || user.locked; //show "disabled" when he "locked" too

    User.get(user, function(user) {
      $scope.selectedUser = user;
      $scope.selectedUser.notifyByEmail = true;
      $scope.selectedUser.disabled = user.disabled || user.locked; //show "disabled" when he "locked" too
      PersonalStorage.exists({guid: user.guid}, function storageExistsCallback(){
          $scope.selectedUser.storage = true;
      });
    }, function(httpResponse) {
      $scope.errors.push(httpResponse.data);
    });
  };

  function update() {
    if (!$scope.selectedUser) {
      return;
    }

    $scope.success = '';
    $scope.errors = [];

    if ($scope.selectedUser.password) {
      if ($scope.selectedUser.password !== $scope.selectedUser.passwordConfirmation) {
        $scope.errors.push('Invalid password confirmation.');
        return
      }
      $scope.selectedUser.passwordConfirmation = '';
    }

    if($scope.selectedUser.generatePassword){
      $scope.selectedUser.password = '';
    }

    User.save($scope.selectedUser, function(user) {
      $scope.success = 'User information successfully updated';
      $scope.selectedUser = user;
      // if list is filtered we can't count on selectedUserIdx
      for (var i = 0; i < $scope.users.length; i++) {
        if ($scope.users[i].guid === user.guid) {
          $scope.users[i] = user;
        }
      }
      PersonalStorage.exists({guid: user.guid}, function storageExistsCallback(){
          $scope.selectedUser.storage = true;
      });
    }, function(httpResponse) {
      var errors = httpResponse.data.errors;
      for (var i = 0; i < errors.length; i++) {
        $scope.errors.push(errors[0].field + ': ' + errors[0].errors[0].message);
      }
    });
  };

  function changeStorage(){
    if($scope.selectedUser.storage){
      // create storage
      PersonalStorage.create({guid: $scope.selectedUser.guid, size: 500}, function(data){
          $scope.selectedUser.storage = true;
      });
    }
    else if(window.confirm("Are you sure? This is an unrecoverable deletion")){
      // delete storage
      PersonalStorage.delete({guid: $scope.selectedUser.guid}, function(data){
          $scope.selectedUser.storage = false;
      });
    }
  };

  function deleteUser(){
    if (!$scope.selectedUser) {
      return;
    }

    var user = $scope.selectedUser;

    var modalInstance = $modal.open({
      templateUrl:       'views/user-management/delete-confirm.html',
      windowTemplateUrl: 'views/not-modal/window.html',
      controller:        'UserDeleteConfirmModalCtrl',
      backdrop:          true,
      scrollableBody:    true,
      resolve: {
        user: function(){return user;}
      }
    });

    modalInstance.result.then(function() {
      var userGuid = user.guid;
      user.$delete(function(){
        //success
        $scope.users = $filter('filter')($scope.users, {guid: '!'+userGuid})
      },function(){
        // error
      });
    });
  }


})
.controller('UserDeleteConfirmModalCtrl', function ($scope, $modalInstance, user) {
  $scope.user = user;
  $scope.del = function(){
    $modalInstance.close();
  };
  $scope.cancel = function(){
    $modalInstance.dismiss('cancel');
  };
});
