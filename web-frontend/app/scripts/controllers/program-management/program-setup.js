'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ProgramManagementProgramSetupCtrl
 * @description
 * # ProgramManagementProgramSetupCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ProgramManagementProgramSetupCtrl', function ($scope, Project, Grant, $modal) {

    $scope.programFilter = {name: ''};
    $scope.success = '';

    $scope.errors = [];

    $scope.selectedProgram = {name: ''};
    $scope.selectedGrant = null;

    $scope.programs = [];
    $scope.grants = [{name:''}];

    $scope.deleteGrant = deleteGrant;
    $scope.deleteProgram = deleteProgram;
    $scope.reset = reset;
    $scope.getGrants = getGrants;
    $scope.add = add;
    $scope.addGrant = addGrant;

    updateList();

    function hasDuplicates(array) {
        var valuesSoFar = Object.create(null);
        for (var i = 0; i < array.length; ++i) {
            var value = array[i];
            if (value in valuesSoFar) {
                return true;
            }
            valuesSoFar[value] = true;
        }
        return false;
    }

    function equalsCaseInsensitive(s1, s2){
      if(s1){
        s1 = s1.toUpperCase();
      }
      if(s2){
        s2 = s2.toUpperCase();
      }
      return s1 === s2
    }

    function add() {
      var grantNames = $scope.grants.map(function(g) { return g.name; });
      if (hasDuplicates(grantNames)) {
        $scope.errors.push('Two or more grants has same name. Please fix it and click save one more time.');
        return;
      }

      $scope.errors = [];
      $scope.success = '';
      if (!$scope.selectedProgram.id) {
        $scope.selectedProgram.key = $scope.selectedProgram.name;

        Project.save($scope.selectedProgram, function(project) {
          var grants2save = $scope.grants;
          // create grants
          for (var i = 0; i < grants2save.length; i++) {
            if (grants2save[i].name.trim().length > 0 && !grants2save[i].id) {
              Project.addGrant(
                {projectId: project.id, name: grants2save[i].name},
                updateGrantOnForm,
                handleErrors
              );
            }
          }

          $scope.success = 'Program ' + project.name + ' successfully created';
          $scope.selectedProgram = {name: ''};
          $scope.grants = [{name:''}];
          updateList();
        }, handleErrors);
      } else {
        // update program
        if( !equalsCaseInsensitive($scope.selectedProgram.name, $scope.selectedProgram.persisted_name) ){
          // update program
          $scope.selectedProgram.$update(function(data, putResponseHeaders) {
                $scope.success = 'Program ' + data.name + ' successfully updated';
                updateProgramOnForm(data);
                $scope.selectedProgram.persisted_name = $scope.selectedProgram.name;
              }, handleErrors)
        }
        // update grants
        var grants2save = $scope.grants;
        for (var i = 0; i < grants2save.length; i++) {
          var grantToSave = new Grant(grants2save[i]);
          if (grantToSave.name.trim().length > 0) {
            if( grantToSave.id && !equalsCaseInsensitive(grantToSave.name, grantToSave.persisted_name) ){
              // update grant
              grantToSave.$update(function(data, putResponseHeaders) {
                $scope.success = 'Grant(s) successfully updated';
                updateGrantOnForm(data);
                addGrantToProgram($scope.selectedProgram.id, data);
                grantToSave.persisted_name = grantToSave.name;
              }, handleErrors);
            }
            else if(!grantToSave.id){

              Project.addGrant({projectId: $scope.selectedProgram.id, name: grantToSave.name}, function(grant){
                $scope.success = 'Grant(s) successfully added';
                updateGrantOnForm(grant);
                addGrantToProgram($scope.selectedProgram.id, grant);
              }, handleErrors);

            }
          }
        }
      }
    };

    function updateProgramOnForm(program) {
      var idx = $scope.programs.map(function(p){ return p.id; }).indexOf(program.id);
      if (idx !== -1) {
        $scope.programs[idx] = program;
      }
    };

    function updateGrantOnForm(grant) {
      var idx = $scope.grants.map(function(g){ return g.name; }).indexOf(grant.name);
      if(idx === -1){
        idx = $scope.grants.map(function(g){ return g.id; }).indexOf(grant.id);
      }
      if (idx !== -1) {
        $scope.grants[idx] = grant;
      }
    };


    function handleErrors(httpResponse) {
      var errors = httpResponse.data ? httpResponse.data.errors : [];
      $scope.errors = $scope.errors.concat( errors.map(function(e){ return e.msg; }) );
    };


    function addGrant() {
      $scope.grants.push({name:''});
    };

    function updateList() {
      Project.query({}, function(programs) {
        $scope.programs = programs;
      });
    };

    function deleteProgram(){
      if($scope.selectedProgram.id){
        var modalInstance = $modal.open({
          templateUrl:       'views/program-management/delete-program-confirm.html',
          windowTemplateUrl: 'views/not-modal/window.html',
          controller:        'DeleteGrantConfirmModalCtrl',
          backdrop:          true,
          scrollableBody:    true,
          resolve: {
            program: function(){return $scope.selectedProgram;},
            grant: function(){return null;}
          }
        });

        modalInstance.result.then(function() {
          $scope.selectedProgram.$delete(function(){
            //success
            updateList();
            reset(true);
          },function(){
            // error
          });
        });
      }
    }

    function deleteGrant(grants, index){
      var grant = new Grant(grants[index]);
      if( grant.id ){
        //delete on server
        var modalInstance = $modal.open({
          templateUrl:       'views/program-management/delete-grant-confirm.html',
          windowTemplateUrl: 'views/not-modal/window.html',
          controller:        'DeleteGrantConfirmModalCtrl',
          backdrop:          true,
          scrollableBody:    true,
          resolve: {
            program: function(){return $scope.selectedProgram;},
            grant: function(){return grant;}
          }
        });

        modalInstance.result.then(function() {
          grant.$delete(function(){
            //success
            grants.splice(index, 1);
            delGrantFromLists(grant);
          },function(){
            // error
          });
        });
      }
      else if(grants.length >= 1){
        grants.splice(index, 1);
      }
    }

    function delGrantFromLists(grant){
      for(var i in $scope.programs){
        var program = $scope.programs[i];
        if(program.id === $scope.selectedProgram.id){
          for(var gi in program.grants){
            var g = program.grants[gi];
            if(g.id === grant.id){
              program.grants.splice(gi, 1);
              break;
            }
          }
          break;
        }
      }
    }

    function addGrantToProgram(programId, grant) {
      var idx = $scope.programs.map(function(p){ return p.id; }).indexOf(programId);
      if (idx !== -1) {
        var grants = $scope.programs[idx].grants;
        // search grant
        idx = grants.map(function(g){ return g.id; }).indexOf(grant.id);
        if(idx !== -1){
          grants[idx] = grant;
        }
        else {
          grants.push(grant);
        }
      }
    };

    function reset(force){
      var justUpdated = false;
      if(!force && $scope.selectedProgram.id){
        var idx = $scope.programs.map(function(p){ return p.id; }).indexOf($scope.selectedProgram.id);
        if (idx !== -1) {
          var program = $scope.programs[idx];

          $scope.selectedProgram = angular.copy(program);
          $scope.grants = angular.copy(program.grants);

          justUpdated = true;
        }
      }

      if(force || !justUpdated){
        $scope.selectedProgram = {name: ''};
        $scope.grants = [{name:''}];
      }

      $scope.success = '';
      $scope.errors = [];
    }

    function getGrants(program){

      // save persisted program name before modification
      if( !program.persisted_name ){
        program.persisted_name = program.name;
      }

      $scope.selectedProgram = angular.copy(program);
      program.expanded = !program.expanded;


      Project.getGrants({id: program.id}, function(grants) {

        // save persisted grant name before modification
        for (var i = 0; i < grants.length; i++) {
          var grant = grants[i];
          if( !grant.persisted_name ){
            grant.persisted_name = grant.name;
          }
        }

        $scope.grants = angular.copy(grants) || [];
        program.grants = grants || [];
      });
    };

  })
  .controller('DeleteGrantConfirmModalCtrl', function ($scope, $modalInstance, program, grant) {
    $scope.program = program;
    $scope.grant = grant;
    $scope.del = function(){
      $modalInstance.close();
    };
    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
  });;
