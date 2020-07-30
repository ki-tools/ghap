'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ProgramManagementAssignmentByGroupCtrl
 * @description
 * # ProgramManagementAssignmentByUserCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ProgramManagementAssignmentByGroupCtrl', function ($scope, User, $timeout, Project, Settings, Group) {

    // $scope.USERS_URL = Settings.API_ROOT + '/group/all/default';

    $scope.groupFilter = {name: ''};
    $scope.programFilter = {name: ''};

    $scope.groups = [];
    $scope.errors = [];
    $scope.selectedGroup = {};
    $scope.programs = [];
    $scope.groupPrograms = [];
    $scope.selectGrants = selectGrants;
    $scope.getAndSelectSelectedGrants = getAndSelectSelectedGrants;

    $scope.getGroupPrograms = getGroupPrograms;
    $scope.selectPrograms = selectPrograms;

    $scope.listDisplayOptions = [{id: 'all', name: 'Show all'},
                        { id: 'enabled', name: 'Show active'},
                        { id: 'disabled', name: 'Show deactive'}];
    $scope.listDisplayMode = $scope.listDisplayOptions[0].id;

    $scope.allGroups = [];

    Group.query(function(groups) {
      $scope.allGroups = groups;

      filterGroupsList();

      Project.query({}, function(programs) {
        $scope.programs = programs;
        $scope.getGroupPrograms(groups[0]);
      });
    });

    function filterGroupsList() {
      $scope.groups = $scope.allGroups
      /*
      $scope.groups = $scope.allGroups.filter(function(group) {
          switch($scope.listDisplayMode) {
            case 'all':
                return true;
            case 'enabled':
                return !group.disabled;
            case 'disabled':
                return group.disabled;
          }

      });
      */
    }

    function selectGrants(program, grants) {
      for (var i = 0; i < program.grants.length; i++) {
        var idx = grants.map(function(x) { return x.id; }).indexOf(program.grants[i].id);
        var grant = program.grants[i];
        grant.selected = (idx !== -1);

        //check program checkbox changes
        if($scope.changedPrograms[program.id] !== undefined){
          grant.selected = program.selected;
        }

        grant = grants[idx];
        if (idx !== -1 && grant.permissions && grant.permissions.length == 1 && grant.permissions[0].toLowerCase() == 'read') {
          $scope.readOnlyGrants[grants[idx].id] = true;
        }
      }
    };

    function getAndSelectSelectedGrants(program){
      program.expanded = !program.expanded;

      if (!program.expanded) {
        return
      }

      Project.getGrants({id: program.id}, function(grants) {
        program.grants = grants || [];
        program.expanded = true;

        if (program.grants.length > 0) {
          for (var i = 0; i < grants.length; i++) {
            $scope.disabledGrants[grants[i].id] = program.selected;// || ($scope.changedPrograms[program.id] !== undefined);
          }

          Project.getGroupProgramGrants({id: program.id, userId: $scope.selectedGroup.guid}, function(grants) {
            $scope.selectGrants(program, grants);
          }, function(httpResponse) {
            $scope.selectGrants(program, [{id:''}]);
          });
        }
      }, function(httpResponse) {
        // TODO catch error
      });
    };

    function selectPrograms(programs) {
      for (var i = 0; i < $scope.programs.length; i++) {
        var idx = programs.map(function(x) { return x.name; }).indexOf($scope.programs[i].name);
        $scope.programs[i].selected = (idx !== -1);
        if (idx !== -1 && programs[idx].permissions.length == 1 && programs[idx].permissions[0].toLowerCase() == 'read') {
          $scope.readOnlyPrograms[programs[idx].id] = true;
        }
      }
    }

    $scope.changedPrograms = [];
    $scope.readOnlyPrograms = [];
    $scope.readOnlyGrants = [];

    function getGroupPrograms(group) {
      $scope.selectedGroup = group;

      $scope.changedPrograms = [];
      $scope.readOnlyPrograms = [];
      $scope.readOnlyGrants = [];
      $scope.changedGrants = [];
      $scope.disabledGrants = [];
      for (var i = 0; i < $scope.programs.length; i++) {
        $scope.programs[i].expanded = false;
      }

      Project.getGroupPrograms({id: $scope.selectedGroup.guid}, function(programs) {
        $scope.selectPrograms(programs);
      }, function(httpResponse) {
        $scope.selectPrograms([{name:''}]);
      });
    }

    $scope.isProgramSelected = function(program) {
      if ($scope.changedPrograms[program.id] !== undefined) {
        return $scope.changedPrograms[program.id];
      }
      return program.selected;
    };

    $scope.selectAndDisableGrants = function(grants) {
      for (var i = 0; i < grants.length; i++) {
        var grant = grants[i];
        $scope.changedGrants[grant.id] = true;
        $scope.disabledGrants[grant.id] = true;
      }
    };

    $scope.toggleProgram = function(program) {
      var val = ($scope.changedPrograms[program.id] !== undefined) ? $scope.changedPrograms[program.id] : program.selected;
      $scope.changedPrograms[program.id] = !val;
      program.selected = !val;

      var idx = $scope.programs.map(function(x) { return x.name; }).indexOf(program.name);

      if ($scope.changedPrograms[program.id]) {
        $scope.programs[idx].expanded = true;
        if (program.grants && program.grants.length > 0) {
          $scope.selectAndDisableGrants(program.grants);
        } else {
          Project.getGrants({id: program.id}, function(grants) {
            $scope.programs[idx].grants = grants;
            $scope.selectAndDisableGrants(program.grants);
          });
        }
      } else {
        var grants = program.grants || [];
        for (var i = 0; i < grants.length; i++) {
          var grant = grants[i];
          $scope.disabledGrants[grant.id] = false;
          if(grant.selected != program.selected){
            grant.selected = program.selected;
            $scope.changedGrants[grant.id] = grant.selected;
          }
        }
      }
    };


    $scope.changedGrants = [];
    $scope.disabledGrants = [];

    $scope.isGrantSelected = function(grant) {
      if ($scope.changedGrants[grant.id] !== undefined) {
        return $scope.changedGrants[grant.id];
      }
      return grant.selected;
    };

    $scope.toggleGrant = function(grant) {
      var val = ($scope.changedGrants[grant.id] !== undefined) ? $scope.changedGrants[grant.id] : grant.selected;
      $scope.changedGrants[grant.id] = !val;
    };

    $scope.savePermissions = function() {
      $scope.success = '';
      $scope.errors = [];

      var successMessage = 'Permission(s) successfully granted/revoked to ' + $scope.selectedGroup.name;

      for (var programId in $scope.changedPrograms) {
        //TODO we need to drop "$scope.changedGrants" for all grants in changed program...or something like it

        var selected = $scope.changedPrograms[programId];
        if (selected) {
          //console.log('granting access to program ' + programId + ' for group ' + $scope.selectedGroup.guid);
          var access = $scope.readOnlyPrograms[programId] ? ['READ'] : ['READ', 'WRITE'];
          //console.log('access', access);
          Project.grantProgramPermissions({id: programId, userId: $scope.selectedGroup.guid}, access, function(result){
            $scope.success = successMessage;
            resetProgramState(programId, true);
          }, function(httpResponse){
            // catch error
            var errors = httpResponse.data ? httpResponse.data.errors : [];
            for (var i = 0; i < errors.length; i++) {
              $scope.errors.push(errors[i].msg);
            }
          });
        } else {
          //console.log('revoking access to program ' + programId + ' for group ' + $scope.selectedGroup.guid);
          Project.revokeProgramPermissions({id: programId, userId: $scope.selectedGroup.guid}, ['READ', 'WRITE'], function(result){
            $scope.success = successMessage;
            resetProgramState(programId, false);
          }, function(httpResponse){
            // catch error
            var errors = httpResponse.data ? httpResponse.data.errors : [];
            for (var i = 0; i < errors.length; i++) {
              $scope.errors.push(errors[i].msg);
            }
          });
        }
      }

      for (var grantId in $scope.changedGrants) {
        selected = $scope.changedGrants[grantId];
        if (selected) {
          //console.log('granting access to grant ' + grantId + ' for group ' + $scope.selectedGroup.guid);
          access = $scope.readOnlyGrants[grantId] ? ['READ'] : ['READ', 'WRITE'];
          //console.log('access', access);
          Project.grantGrantPermissions({id: grantId, userId: $scope.selectedGroup.guid}, access, function(result){
            // handle success
            if($scope.success === ''){
              $scope.success = successMessage;
            }
            resetGrantState(grantId, true);
          }, function(httpResponse){
            // TODO catch error
          });
        } else {
          //console.log('revoking access to grant ' + grantId + ' for group ' + $scope.selectedGroup.guid);
          Project.revokeGrantPermissions({id: grantId, userId: $scope.selectedGroup.guid}, ['READ', 'WRITE'], function(result){
            // handle success
            if($scope.success === ''){
              $scope.success = successMessage;
            }
            resetGrantState(grantId, false);
          }, function(httpResponse){
            // TODO catch error
          });
        }
      }
    };

    function resetGrantState(grantId, selected){
      delete $scope.changedGrants[grantId];
      for(var i=0;i < $scope.programs.length;i++){

        var program = $scope.programs[i];
        var grants = program.grants || [];

        for (var ig = 0; ig < grants.length; ig++) {
          var grant = grants[ig];
          if(grantId === grant.id){
            grant.selected = selected;
          }
        }
      }
    }

    function resetProgramState(programId, selected){
      delete $scope.changedPrograms[programId];
      for(var i=0;i < $scope.programs.length;i++){

        var program = $scope.programs[i];
        if(programId === program.id){
          program.selected = selected;
          /*
          var grants = program.grants || [];

          for (var ig = 0; ig < grants.length; ig++) {
            var grant = grants[ig];
            grant.selected = selected;
          };
          */
        }
      }
    }

    $scope.toggleReadOnlyProgram = function(program) {
      $scope.changedPrograms[program.id] = true;
      $scope.readOnlyPrograms[program.id] = !$scope.readOnlyPrograms[program.id];

      if ($scope.readOnlyPrograms[program.id]) {
        for (var i = 0; i < program.grants.length; i++) {
          var grant = program.grants[i];
          $scope.changedGrants[grant.id] = true;
          $scope.readOnlyGrants[grant.id] = true;
        }
      }
    };

    $scope.toggleReadOnlyGrant = function(grant) {
      $scope.changedGrants[grant.id] = true;
      $scope.readOnlyGrants[grant.id] = !$scope.readOnlyGrants[grant.id];
    };

  });
