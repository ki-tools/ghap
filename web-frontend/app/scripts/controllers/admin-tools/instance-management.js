'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:InstanceManagementCtrl
 * @description
 * # InstanceManagementCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('InstanceManagementCtrl', function ($scope, $timeout, Settings, User, Stack, $location, $interval, $modal) {

    $scope.isLoaded = false;

    $scope.stacks = [];
    $scope.user = null;
    $scope.search = {user: {name: ''}};
    $scope.activities = [];
    $scope.modelActivities = [];
    $scope.pendingStacks = [];
    $scope.stacksChecked = 0;
    $scope.rdps = [];

    $scope.removeById = removeById;
    $scope.push2Pending = push2Pending;
    $scope.getActivityById = getActivityById;
    $scope.getStacks = getStacks;
    $scope.computeResources = computeResources;
    $scope.initActivities = initActivities;

    $scope.terminate = terminate;
    $scope.pause = pause;
    $scope.resume = resume;
    $scope.toggleStack = toggleStack;
    $scope.isOn = isOn;
    $scope.isDisabled = isDisabled;
    $scope.isVPG = isVPG;

    activate();

    function activate(){
      User.getCurrentUser(function getCurrentUserCallback(user){
        $scope.user = user;
        User.getActivities($scope.initActivities);
      });
    };

    /////////////////////////////////////////////////////

    function removeById(arr, id) {
      var idx = arr.map(function(i){ return i.id; }).indexOf(id);
      if (idx !== -1) {
        arr.splice(idx, 1);
      }
      return arr;
    };

    function push2Pending(activity) {
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

    function getActivityById(activityId) {
      var idx = $scope.internalActivities.map(function(a) { return a.id; }).indexOf(activityId);
      return (idx !== -1) ? $scope.internalActivities[idx] : null;
    };

    function getStacks(){

      Stack.query({guid: null}, function(stacks) {
        // if no stacks show all activities
        if (stacks.length === 0 && $scope.stacks.concat($scope.pendingStacks).length === 0) {
          $scope.activities = angular.copy($scope.internalActivities);
          return;
        }

        Stack.computeResources(null, function(resources) {
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
            if (r.address) {
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

          var userIds = [];
          var stackList = stacks.concat($scope.pendingStacks);
          for(var i = 0; i < stackList.length; i++){
            userIds.push(stackList[i].userId);
          };
          User.reportUsers(userIds,
            function(users){
              $scope.isLoaded = true;

              var userMap = [];
              for(var i = 0; i < users.length; i++){
                var user = users[i];
                userMap[user.userid] = user;
              };
              for(var i = 0; i < stackList.length; i++){
                var stack = stackList[i];
                stack.user = userMap[stack.userId];
              };
              $scope.stacks = stacks;
            },
            function(data, status){
              //console.error("Cannot retrieve user report. See logs.", data)
            }
          );
        }, function(){});
      });
    };

    function computeResources(){
      if ($location.path() !== '/admin-tools/instance-management') {
        return;
      }
      $scope.getStacks();

      $interval($scope.computeResources, 60000, 1, true);
    };

    function isVPG(stack){
      return stack.resources && (stack.resources.main.autoScaleMaxInstanceCount || (stack.resources.all && stack.resources.all.length > 1));
    }

    function initActivities(activities) {
      activities.forEach(function(a){
        if (a.os === undefined) {
          a.os = '';
        }
      });
      $scope.internalActivities = angular.copy(activities);
      $scope.computeResources();
    };

    function terminate(stack){
      var activityId = stack.activityId;
      var userid = stack.user.userid
      var modalInstance = $modal.open({
        templateUrl:       'views/admin-tools/decomissioning-env-confirm.html',
        windowTemplateUrl: 'views/not-modal/window.html',
        controller:        'DecomissionConfirmModalCtrl',
        backdrop:          true,
        scrollableBody:    true,
        resolve: {
          user: function(){return stack.user;},
          resource: function(){return stack.resources.main;}
        }
      });

      modalInstance.result.then(function() {
        Stack.terminate(userid, activityId);
        $timeout($scope.getStacks, 1000, true);
      });
    }

    function pause(activityId, userid) {
      Stack.pause({guid: userid}, $scope.getActivityById(activityId));
      $timeout($scope.getStacks, 1000, true);
    }

    function resume(activityId, userid) {
      Stack.resume({guid: userid}, $scope.getActivityById(activityId));
      $timeout($scope.getStacks, 1000, true);
    }

    function toggleStack(stack) {
      var activityId = stack.activityId;
      var resource = stack.resources.main;
      var userid = stack.user.userid;

      if ($scope.isDisabled(resource)) {
        return;
      }

      if (resource.status === 'running') {
        var modalInstance = $modal.open({
          templateUrl:       'views/admin-tools/pause-env-confirm.html',
          windowTemplateUrl: 'views/not-modal/window.html',
          controller:        'DecomissionConfirmModalCtrl',
          backdrop:          true,
          scrollableBody:    true,
          resolve: {
            user: function(){return stack.user;},
            resource: function(){return resource;}
          }
        });

        modalInstance.result.then(function() {
          $scope.pause(activityId, userid);
          resource.status = 'stopping';
        });

      } else {
        $scope.resume(activityId, userid);
        resource.status = 'pending';
      }
    };

    function isOn(resource) {
      return ['pending', 'initializing', 'running'].indexOf(resource.status) !== -1;
    };

    function isDisabled(resource) {
      return ['stopped', 'running'].indexOf(resource.status) === -1;
    };

  })
  .controller('DecomissionConfirmModalCtrl', function ($scope, $modalInstance, user, resource) {
    $scope.user = user;
    $scope.resource = resource;
    $scope.del = function(){
      $modalInstance.close();
    };
    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
  });
