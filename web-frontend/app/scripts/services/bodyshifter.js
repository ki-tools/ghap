'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.BodyShifter
 * @description
 * # BodyShifter
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('BodyShifter', function BodyShifter() {

    function shift() {
      $(document.body).css({
        "padding-top": ($('#fixed-header').height() - $('#feedback').height()) + 'px'
      });
    }

    return {
      shift: shift
    }
  });
