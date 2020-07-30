'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.auth
 * @description
 * # auth
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
.factory('Auth', function (User) {

  var authorize = function (loginRequired, requiredRoles, roleCheckType, callback) {
    var loggedIn = User.isUserLoggedIn();

    loginRequired = loginRequired || (requiredRoles !== undefined && requiredRoles.length > 0);
    roleCheckType = roleCheckType || 'atLeastOne';

    if (loginRequired === true && !loggedIn) {
      callback('loginRequired');
    } else if ((loginRequired === true && loggedIn) &&
              (requiredRoles === undefined || requiredRoles.length === 0)) {
      // Login is required but no specific roles are specified.
      callback('authorised');
    } else if (requiredRoles) {
      if( !loggedIn ){
        callback('loginRequired');
      } else {
        User.getCurrentUserRoles(function(roles){
          var loweredRoles = [];
          angular.forEach(roles, function (roles) {
            loweredRoles.push(roles.name.toLowerCase());
          });

          var role;
          var hasRole = false;
          for (var i = 0; i < requiredRoles.length; i += 1) {
            role = requiredRoles[i].toLowerCase();

            if (roleCheckType === 'combinationRequired') {
              hasRole = hasRole && loweredRoles.indexOf(role) > -1;
              // if all the roles are required and hasRole is false there is no point carrying on
              if (hasRole === false) {
                break;
              }
            } else if (roleCheckType === 'atLeastOne') {
              hasRole = loweredRoles.indexOf(role) > -1;
              // if we only need one of the roles and we have it there is no point carrying on
              if (hasRole) {
                break;
              }
            }
          }

          var result = hasRole ? 'authorised' : 'notAuthorised';
          callback(result);
        });
      }
    } else {
      callback('authorised');
    }
  };

  return {
    authorize: authorize
  };

});
