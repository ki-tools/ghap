'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:PasswordResetCtrl
 * @description
 * # PasswordResetCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('PasswordResetCtrl', function ($scope, User, $http, Settings) {

    User.dropCurrentUserRoles();

    $scope.getToken = getToken;
    $scope.reset = reset;

    function getToken() {
      var hash = window.location.hash.replace('?', '&');
      if (hash.indexOf('&token=') !== -1) {
        var pairs = hash.split('&');
        for (var i = 0; i < pairs.length; i++) {
          if (pairs[i].indexOf('token=') === 0) {
            var keyValue = pairs[i].split('=');
            return keyValue[1];
          }
        }
      }
      return null;
    }

    function pushErrors(ent){
      for (var i2 = 0; i2 < ent.errors.length; i2++) {
        var error = ent.errors[i2];
        var marker = 'check_password_restrictions:';

        if(ent.field === 'password' && error.message.indexOf(marker) >= 0){
          var pos = error.message.indexOf(marker);
          var msg = error.message.substring(pos+marker.length);
          $scope.errors.push(msg);
        } else if(ent.field === 'password' && error.message.indexOf('DSID-03191083') >= 0){
          $scope.errors.push('You tried to change password to one that is saved in history');
        } else {
          $scope.errors.push(ent.field + ': ' + error.message);
        }
      }
    }

    function processErrors(data){
      var errors = (data && data.errors) ? data.errors : [];

      for (var i = 0; i < errors.length; i++) {
        var ent = errors[i];// contains field related errors
        if (ent.field === 'password') {
          switch (ent.errors[0].code) {
            case 'constraintViolation':
              pushErrors(ent);
              break;
            case 'invalidCredentials':
              $scope.errors.push('Current Password is invalid');
              break;
            default:
              $scope.errors.push('Password is invalid');
              $scope.errors.push('Refer to password requirements on page');
          }
        } else {
          pushErrors(ent);
        }
      }
    }

    function reset() {
      $scope.errors = [];

      /*jshint validthis:true */
      var password = $scope.password || this.password;
      var passwordConfirm = $scope.passwordConfirm || this.passwordConfirm;

      if (!password) {
        $scope.errors = ['Enter password'];
        return;
      }

      if (password !== passwordConfirm) {
        $scope.errors = ['Passwords are different'];
        return;
      }

      var token = $scope.getToken();
      if (token) {
        User.save({action: 'password', dn: 'token', forgotPasswordEmail: token}, password, function(user) {
          /*jshint unused:false*/
          if(window.alert){
            window.alert('Password was reset. Now you will be redirected to login page.');
          }
          document.location.href = Settings.OAUTH_URL + '/oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=' + document.location.href.replace('/password-reset', '');
        }, function(httpResponse){
          processErrors(httpResponse.data);
        });
      }
      else {
        var resetPassword = function resetPassword(user) {
          var url = Settings.API_ROOT + '/user/password/reset/' + (user ? encodeURIComponent(user.dn):'');
          $http
            .post(url, {password: password})
            .success(function() {
              $http.get(Settings.OAUTH_URL + '/oauth/revoke').success(function() {
                User.dropCurrentUser();
                document.location.href = Settings.OAUTH_URL + '/oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=' + document.location.href.replace('/password-reset', '');
              });
            })
            .error(processErrors);
        };

        User.getCurrentUser(resetPassword, resetPassword);
      }

    }

  });
