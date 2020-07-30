'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:modaldraggable
 * @description
 * # modaldraggable
 */
angular.module('bmgfApp')
  .directive('modaldraggable', function ($document) {
  return function (scope, element) {
    var startX = 0,
      startY = 0,
      x = 0,
      y = 0;
    var header= angular.element(document.getElementsByClassName('modal-header'));
    header.css({
      position: 'relative',
      cursor: 'move'
    });
    element= angular.element(document.getElementsByClassName('modal-dialog'));
    var forCenterEl = angular.element(document.getElementsByClassName('for-center'));

    header.on('mousedown', function (event) {
      // Prevent default dragging of selected content
      event.preventDefault();

      // check close button in a header
      var closeButton = $(event.target).attr('aria-hidden') === 'true';
      if(closeButton){
        return;
      }

      var position = event.target.getBoundingClientRect();

      startX = event.screenX - position.left;
      startY = event.screenY - position.top+46;

      $document.on('mousemove', mousemove);
      $document.on('mouseup', mouseup);
    });

    function mousemove(event) {
      y = event.screenY - startY;
      x = event.screenX - startX;

      //var position = event.target.getBoundingClientRect();

      forCenterEl.css({
        left: 0
      });

      element.css({
        top: y + 'px',
        left: x + 'px'
      });
    }

    function mouseup() {
      $document.unbind('mousemove', mousemove);
      $document.unbind('mouseup', mouseup);
    }
  };
});
