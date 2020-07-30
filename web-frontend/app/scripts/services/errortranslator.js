'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.errorTranslator
 * @description
 * # errorTranslator
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('ErrorTranslator', function Errortranslator() {
        function capitalizeFirstLetter(string) {
          return string.charAt(0).toUpperCase() + string.slice(1);
        }

        var pushErrors = function pushErrors(translatedErrors, ent){
          for (var i2 = 0; i2 < ent.errors.length; i2++) {
            var error = ent.errors[i2];
            var marker = 'check_password_restrictions:';

            if(ent.field === 'password'){
              if(error.message.indexOf(marker) >= 0){
                var pos = error.message.indexOf(marker);
                var msg = error.message.substring(pos+marker.length);
                translatedErrors.push(msg);
              }
              else if(error.message.indexOf('DSID-03191083') >= 0){
                translatedErrors.push('You tried to change password to one that is saved in history');
              }
              else if(error.code === 'Uppercase'){
                translatedErrors.push('Password should contain: Uppercase alpha characters (A-Z)');
              }
              else if(error.code === 'PasswordLength'){
                translatedErrors.push('Password should be a minimum of ' + error.message + ' characters in length');
              }
              else if(error.code === 'Lowercase'){
                translatedErrors.push('Password should contain: Lowercase alpha characters (a-z)');
              }
              else if(error.code === 'NonAlphanumeric'){
                translatedErrors.push('Password should contain: Non-alphanumeric characters (e.g. !@#$%^&*(){}[]`~)');
              }
              else if(error.code === 'Digits'){
                translatedErrors.push('Password should contain: Digits (0-9)');
              }
              else {
                translatedErrors.push('Password: ' + error.message);
              }
            } else {
              translatedErrors.push(capitalizeFirstLetter(ent.field) + ': ' + error.message);
            }
          }
        };



        return {
          populate: function populate(errors){
            var translatedErrors = [];
            for (var i = 0; i < errors.length; i++) {
              var ent = errors[i];// contains field related errors
              if (ent.field === 'password') {
                switch (ent.errors[0].code) {
                  case 'constraintViolation':
                    pushErrors(translatedErrors, ent);
                    break;
                  case 'invalidCredentials':
                    translatedErrors.push('Current Password is invalid');
                    break;
                  default:
                    //pushErrors(translatedErrors, ent);
                    translatedErrors.push('Password is invalid');
                    translatedErrors.push('Refer to password requirements on page');
                }
              } else {
                pushErrors(translatedErrors, ent);
              }

              return translatedErrors;
            }
          }
        };
  });
