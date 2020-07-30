'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.User
 * @description
 * # User
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
.factory('User', function ($resource, $location, Activity, ActivityRoleAssociation, Settings, SyncHttp, $http) {

  var res = $resource(Settings.API_ROOT + '/user/:action/:dn/:forgotPasswordEmail',
    { dn:'@dn', action:'@action', forgotPasswordEmail:'@forgotPasswordEmail' },
    {
      list: {method: 'GET', params: {action: 'all', dn: 'default'}, cache: false, isArray: true},
      delete: {method: 'DELETE', params: {action: null}, cache: false}
      // roles:  {method: 'GET', params: {action: 'roles'}, cache: false, isArray: true},
      // groups: {method: 'GET', params: {action: 'groups', dn: '@dn'}, cache: false, isArray: true}
    }
  );

  res.dropCurrentUserRoles = function(callback) {
    localStorage.removeItem('current_user_roles');
    if (callback) {
      callback();
    }
  };

  res.dropCurrentUser = function(callback) {
    localStorage.removeItem('current_user');
    localStorage.removeItem('access_token');
    res.dropCurrentUserRoles(callback);
  };

  /*
  var initJira = function(user){
    window.ATL_JQ_PAGE_PROPS = $.extend(window.ATL_JQ_PAGE_PROPS, {
      // ==== custom trigger function ====
      triggerFunction: function( showIssueCollector ) {
        //may be we need to update JIRA to make "triggerFunction" works
      },
      // ==== we add the code below to set the field values ====
      fieldValues: {
          email : user.email,
          fullname : user.name
      }
    });
  };
  */

  (function(){
    // get current user sync way
    var currentUserRoles = null;
    var currentUser = SyncHttp.get(Settings.API_ROOT + '/user');
    // Safari 8 fix see BAP-814 for more info
    if (currentUser && currentUser.indexOf('<') === 0) {
      //currentUser = SyncHttp.get(Settings.API_ROOT + '/user');
      location.reload();
    }
    if (currentUser){
      localStorage.setItem('current_user', currentUser);
      try {
        currentUser = JSON.parse(currentUser);
      } catch (e) {
        //console.log(e);
        throw 'Can\'t parse user from SyncHttp.get(\'/user\')';
      }
      //initJira(currentUser);

      // get roles sync way
      currentUserRoles = SyncHttp.get(Settings.API_ROOT + '/user/roles/' + encodeURIComponent(currentUser.dn));
      if( currentUserRoles !== null ){
        localStorage.setItem('current_user_roles', currentUserRoles);
        currentUserRoles = JSON.parse(currentUserRoles);
      } else {
        //console.error('Request for user roles returns error. See logs above');
        res.dropCurrentUserRoles();
      }
    } else {
      res.dropCurrentUser();
    }
  })();


  res.reportUsers = function reportUsers(ids, success, error){
      var params = [];
      if( angular.isString(ids) ){
        if( ids.indexOf(',') >= 0){
          ids = ids.split(',');
        } else {
          ids = [ids];
        }
      }
      for(var i in ids){
        params.push( 'guid=' + ids[i]);
      }

      $http.get(Settings.API_ROOT + '/report/users?' + params.join('&'), {cache: false}).success(success).error(error);
  };

  res.isUserLoggedIn = function() {
    var token = localStorage.getItem('access_token');
    var isLoggedIn = (token !== undefined && token !== null);
    if (!isLoggedIn) {
      isLoggedIn = (window.location.hash.indexOf('access_token') !== -1);
    }
    return isLoggedIn;
  };

  res.setCurrentUser = function(user) {
    var json = angular.isString(user) ? user : JSON.stringify(user);
    localStorage.setItem('current_user', json);
  };

  res.isResetPassword = function(){
    var currentUser = localStorage.getItem('current_user');
    if(currentUser){
      currentUser = JSON.parse(currentUser);
      return currentUser.resetPassword;
    } else {
      return false;
    }
  };

  res.accessDeniedRedirect = function(){
    if ( res.isResetPassword() ) {
        $location.path('/password-reset').replace();
    }
    else {
      $location.path('/access-denied').replace();
    }
  };

  res.getCurrentUser = function(callback, notSignedinCallback) {
    var currentUser = localStorage.getItem('current_user');
    if ( currentUser ) {
      if(callback){
        callback(JSON.parse(currentUser));
      }
    }
    else if(notSignedinCallback){
      notSignedinCallback();
    }
  };

  res.getCurrentUserRoles = function(callback) {
    // callback([{guid: '00000000-0000-0000-0000-000000000000', name: 'BMGF Administrator'}]);
    var currentUserRoles = localStorage.getItem('current_user_roles');
    if (currentUserRoles) {
      callback(JSON.parse(currentUserRoles));
    } else {
      callback([]);
    }
  };

  res.getActivities = function(callback) {

    res.getCurrentUserRoles(function roleQueryCallback(roles) {

      var loadActivity = function loadActivity(association, callback){
        Activity.getById({id: association.activityId}, function(activity){
          callback(null, activity);
        }, function(){
          callback(null);
        });
      };

      var loadActivities = function loadActivities(role, callback){
        ActivityRoleAssociation.query( {guid: role.guid}, function activityRoleAssociationCallback(activitesForRole){
          async.map(activitesForRole, loadActivity, function(e,r){
            callback(null, r);
          });
        }, function(){
          callback(null);
        });
      };

      async.map(roles, loadActivities, function(e,r){
        var activities = (r === null || r.length === 0) ? [] : r.reduce(function(a, b) {
          return (a !== undefined) ? a.concat(b) : b;
        });
        
        var uniqIds = [];
        var uniqArray = [];

        for(var i in activities){
          var a = activities[i];
          if( a && uniqIds.indexOf(a.id) < 0 ){
            uniqIds.push(a.id);
            uniqArray.push(a);
          }
        }

        callback(uniqArray);
      });
    });

  };

  return res;

});
