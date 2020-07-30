'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ProgramManagementAssignmentByProgramCtrl
 * @description
 * # ProgramManagementAssignmentByProgramCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ProgramManagementAssignmentByProgramCtrl', function ($scope, User, $timeout, Project, Settings) {

    // $scope.USERS_URL = Settings.API_ROOT + '/user/all/default';

    $scope.userFilter = {name: ''};

    $scope.programFilter = {name: ''};

    $scope.users = [];
    $scope.userGuids = [];
    $scope.errors = [];
    $scope.programUsers = [];

    $scope.selectedProgram = {};
    $scope.selectedGrant = {};
    $scope.getProgramUsers = getProgramUsers;
    $scope.resetSelection = resetSelection;
    $scope.selectUsers = selectUsers;

    Project.query({}, function(programs) {
      $scope.programs = programs;
      User.list(function(users) {
        $scope.users = users;
        $scope.userGuids = users.map(function(it) { return it.guid; });
        $scope.getProgramUsers($scope.programs[0]);
      });
      Project.getGrants({id: $scope.programs[0].id}, function(grants) {
        $scope.programs[0].grants = grants;
      });
    });


    $scope.changedUsers = [];
    $scope.readOnlyUsers = [];

    $scope.isUserSelected = function(user) {
      if ($scope.changedUsers[user.guid] !== undefined) {
        return $scope.changedUsers[user.guid];
      }
      return user.selected;
    };

    $scope.toggleUser = function(user) {
      var val = ($scope.changedUsers[user.guid] !== undefined) ? $scope.changedUsers[user.guid] : user.selected;
      $scope.changedUsers[user.guid] = !val;
    };

    $scope.collectErrors = function(httpResponse){
      var errors = httpResponse.data ? httpResponse.data.errors : [];
      for (var i = 0; i < errors.length; i++) {
        $scope.errors.push(errors[i].msg);
      }
    };

    $scope.savePermissionsProject = function(programm){
      var successMessage = 'Program "' + programm.name + '" was successfully assigned to user(s) ';

      for (var userGuid in $scope.changedUsers) {
        var selected = $scope.changedUsers[userGuid];
        if (selected) {
          var access = $scope.readOnlyUsers[userGuid] ? ['READ'] : ['READ', 'WRITE'];
          //console.log('granting access to program ' + programm.id + ' for user ' + userGuid);
          Project.grantProgramPermissions({id: programm.id, userId: userGuid}, access, function(result){
            $scope.success = successMessage;
          }, $scope.collectErrors);
        } else {
          //console.log('revoking access to program ' + programm.id + ' for user ' + userGuid);
          Project.revokeProgramPermissions({id: programm.id, userId: userGuid}, ['READ', 'WRITE'], function(result){
            $scope.success = successMessage;
          }, $scope.collectErrors);
        }
      }
    };

    $scope.savePermissionsGrant = function(grant){
      var successMessage = 'Grant "' + grant.name + '" was successfully assigned/revoked to user(s) ';

      for (var userGuid in $scope.changedUsers) {
        var selected = $scope.changedUsers[userGuid];
        if (selected) {
          var access = $scope.readOnlyUsers[userGuid] ? ['READ'] : ['READ', 'WRITE'];
          //console.log('granting access to grant ' + grant.id + ' for user ' + userGuid);
          Project.grantGrantPermissions({id: grant.id, userId: userGuid}, access, function(result){
            $scope.success = successMessage;
          }, $scope.collectErrors);
        } else {
          //console.log('revoking access to grant ' + grant.id + ' for user ' + userGuid);
          Project.revokeGrantPermissions({id: grant.id, userId: userGuid}, ['READ', 'WRITE'], function(result){
            $scope.success = successMessage;
          }, $scope.collectErrors);
        }
      }
    };

    $scope.savePermissions = function() {
      $scope.success = '';
      $scope.errors = [];

      if ($scope.selectedProgram.id) {
        $scope.savePermissionsProject($scope.selectedProgram);
      } else if ($scope.selectedGrant.id) {
        $scope.savePermissionsGrant($scope.selectedGrant);
      }
    };

    $scope.toggleReadOnlyUser = function(user) {
      $scope.changedUsers[user.guid] = true;
      $scope.readOnlyUsers[user.guid] = !$scope.readOnlyUsers[user.guid];
    };

    $scope.getGrantUsers = function(grant) {
      $scope.resetSelection();
      $scope.selectedGrant = grant;
      Project.getProgramGrantUsers({entityName: grant.id}, function getProgramGrantUsersCallback(grantUsers) {
        Project.getProgramUsers({id: grant.programId}, function(programUsers) {
          $scope.programUsers = programUsers;
          selectUsers(grantUsers);
        }, function(httpResponse) {
          $scope.programUsers = [];
          selectUsers(grantUsers);
        });
      });
    };

    ////////////////////////////////////////////////////////////
    function disableUsers(){
      $scope.users.forEach(function(it) { it.disabled = false }); //enable all users

      var programId = $scope.selectedGrant.programId;
      if(programId){
        // if Grant selected - disable users which presents in program
        $scope.programUsers.forEach(function(it){
          var idx = $scope.userGuids.indexOf(it.guid);
          if (idx !== -1){
            $scope.users[idx].disabled = true;
          }
        });
      }
    }

    function selectUsers(users) {
      for (var i = 0; i < $scope.users.length; i++) {
        var idx = users.map(function(x) { return x.guid; }).indexOf($scope.users[i].guid);
        $scope.users[i].selected = (idx !== -1);
        if (idx !== -1 && users[idx].permissions.length == 1 && users[idx].permissions[0].toLowerCase() == 'read') {
          $scope.readOnlyUsers[users[idx].guid] = true;
        }
      }
      disableUsers();
    };

    function getProgramUsers(program) {
      program.expanded = !program.expanded;
      $scope.resetSelection();
      $scope.selectedProgram = program;
      // TODO get list of users
      Project.getProgramUsers({id: program.id}, function(users) {
        $scope.selectUsers(users);
        $scope.programUsers = users;
      }, function(httpResponse) {
        $scope.selectUsers([{guid:''}]);
      });
      Project.getGrants({id: program.id}, function(grants) {
        for (var i = 0; i < grants.length; i++) {
          grants[i].programId = program.id;
        }
        program.grants = grants;
      });
    };

    function resetSelection() {
      $scope.selectedProgram = {};
      $scope.selectedGrant = {};
      $scope.changedUsers = [];
      $scope.readOnlyUsers = [];
      $scope.programUsers = [];
    };


  });
