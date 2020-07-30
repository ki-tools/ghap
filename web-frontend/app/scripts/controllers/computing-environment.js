'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ComputingEnvironmentCtrl
 * @description
 * # ComputingEnvironmentCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ComputingEnvironmentCtrl', function ($scope, $timeout, Settings, User, Stack, $location, $interval) {

    $scope.stacks = [];
    $scope.user = null;
    $scope.activities = [];
    $scope.modelActivities = [];
    $scope.pendingStacks = [];
    $scope.stacksChecked = 0;
    $scope.rdps = [];
    $scope.isDisabled = isDisabled;
    $scope.isVPG = isVPG;
    $scope.isOn = isOn;

    $scope.removeById = function(arr, id) {
      var idx = arr.map(function(i){ return i.id; }).indexOf(id);
      if (idx !== -1) {
        arr.splice(idx, 1);
      }
      return arr;
    };

    $scope.push2Pending = function(activity) {
      var idx = $scope.pendingStacks.map(function(s){ return s.activityId; }).indexOf(activity.id);
      if (idx === -1) {
        $scope.pendingStacks.push({
          activityId: activity.id,
          resources: {
            main: {
              instanceOsType: activity.os.replace(/^[a-z]/, function(m){ return m.toUpperCase(); }),
              instanceId:     '.',
              address:        '.',
              coreCount:      '.',
              status:         'initializing',
            },
            all: [],
          }
        });
      }
    };

    $scope.getActivityById = function(activityId) {
      var idx = $scope.internalActivities.map(function(a) { return a.id; }).indexOf(activityId);
      return (idx !== -1) ? $scope.internalActivities[idx] : null;
    };

    $scope.getStacks = function(){
      
      Stack.query({guid: $scope.user.guid}, function(stacks) {
        // if no stacks show all activities
        if (stacks.length === 0 && $scope.stacks.concat($scope.pendingStacks).length === 0) {
          $scope.activities = angular.copy($scope.internalActivities);
          return;
        }

        Stack.computeResources($scope.user, function(resources) {
          var oss = [];
          var i, idx;

          for (i = 0; i < resources.length; i++) {
            var r = resources[i];

            // put resource to stack
            idx = stacks.map(function(s) { return s.stackId; }).indexOf(r.stackId);
            if (!stacks[idx].resources) {
              stacks[idx].resources = {main: r, all: []};
            }
            stacks[idx].resources.all.push(r);
            if (r.isTopLevelNode) {
              stacks[idx].resources.main = r;
            }

            oss.push(r.instanceOsType.toLowerCase());
            if (r.instanceOsType.toLowerCase() === 'windows' && r.status === 'running'){
              $scope.rdps[r.instanceId] = Stack.rdpFileUrl(r);
            }
          }

          // removing empty stacks
          stacks = stacks.filter(function(s) { return s.resources && s.resources.main; });

          // calc cores count and remove pendings
          for (i = 0; i < stacks.length; i++) {
            /*
            stacks[i].resources.main.coreCount = stacks[i].resources.all
              .map(function(r) { return r.coreCount; })
              .reduce(function(pv, cv) { return pv + cv; }, 0);
            */
            idx = $scope.pendingStacks.map(function(s) { return s.activityId; }).indexOf(stacks[i].activityId);
            if (idx !== -1) {
              $scope.pendingStacks.splice(idx, 1);
            }
          }

          // adding oss from pending activities
          var poss = $scope.pendingStacks.map(function(s) { return s.resources.main.instanceOsType.toLowerCase(); });
          oss = oss.concat(poss);
          var activities = angular.copy($scope.internalActivities);
          // remove from activities oss
          $scope.activities = activities.filter(function(a) {
            return oss.indexOf(a.os.toLowerCase()) === -1;
          });

          $scope.stacks = stacks;
        }, function(){});
      });
    };

    $scope.computeResources = function(){
      if ($location.path() !== '/computing-environment') {
        return;
      }
      $scope.getStacks();
      
      $interval($scope.computeResources, 10000, 1, true);
    };

    $scope.initActivities = function(activities) {
      activities.forEach(function(a){
        if (a.os === undefined) {
          a.os = '';
        }
      });
      $scope.internalActivities = angular.copy(activities);
      $scope.computeResources();
    };

    User.getCurrentUser(function getCurrentUserCallback(user){
      $scope.user = user;
      User.getActivities($scope.initActivities);
    });

    $scope.create = function(){
      $scope.showVirtEnvDialog = false;
      var activities = [];
      var oss = [];
      for (var activityId in $scope.modelActivities) {
        if (!$scope.modelActivities[activityId]) {
          continue;
        }

        // removing selected activity
        var idx = $scope.activities.map(function(a){ return a.id; }).indexOf(activityId);
        var act = $scope.activities[idx];
        oss.push(act.os.toLowerCase());
        activities.push(act);
        $scope.push2Pending(act);
        // $scope.activities.splice(idx, 1);

        // // removing activity with same os if there any
        // idx = $scope.activities.map(function(a){ return a.os; }).indexOf(act.os);
        // if (idx !== -1) {
        //   $scope.activities.splice(idx, 1);
        // }
      }
      $scope.activities = $scope.activities.filter(function(a){
        return oss.indexOf(a.os.toLowerCase()) === -1;
      });
      $scope.modelActivities = {};
      Stack.create({guid: $scope.user.guid}, activities);
      $timeout($scope.getStacks, 1000, true);
    };

    $scope.terminate = function(activityId){
      Stack.terminate($scope.user.guid, activityId);
      $timeout($scope.getStacks, 1000, true);
    };

    $scope.pause = function(activityId) {
      Stack.pause({guid: $scope.user.guid}, $scope.getActivityById(activityId));
      $timeout($scope.getStacks, 1000, true);
    };

    $scope.resume = function(activityId) {
      Stack.resume({guid: $scope.user.guid}, $scope.getActivityById(activityId));
      $timeout($scope.getStacks, 1000, true);
    };

    $scope.toggleStack = function(activityId, resource) {
      if ($scope.isDisabled(resource)) {
        return;
      }

      if (resource.status === 'running') {
        $scope.pause(activityId);
        resource.status = 'stopping';
      } else {
        $scope.resume(activityId);
        resource.status = 'pending';
      }
    };

    function isOn(resource) {
      return ['pending', 'initializing', 'running'].indexOf(resource.status) !== -1;
    }

    function isDisabled(resource) {
      return ['stopped', 'running'].indexOf(resource.status) === -1;
    }

    function isVPG(stack){
      return stack.resources && (stack.resources.main.autoScaleMaxInstanceCount || (stack.resources.all && stack.resources.all.length > 1));
    }


  });
