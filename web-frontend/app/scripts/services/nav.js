'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.nav
 * @description
 * # nav
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Nav', function (User, Role, $location, paths, $http, Settings) {

    var menus = [{
        name: 'User<br>Management',
        url: '/user-management/edit-account',
        submenus: [
          {
            name: 'Create Account',
            url: '/user-management/create-account'
          },
          {
            name: 'Edit Account',
            url: '/user-management/edit-account'
          },
          {
            name: 'Manage Permissions',
            url: '/user-management/manage-permissions'
          }
        ]
      },
      {
        name: 'Role<br>Management',
        url: '/role-management/create-roles',
        submenus: [
          {
            name: 'Create Roles',
            url: '/role-management/create-roles'
          },
          {
            name: 'Assign Modeling Activities',
            url: '/role-management/assign-modeling-activities'
          }
        ]
      },
      {
        name: 'Group<br>Management',
        url: '/group-management/create-groups',
        submenus: [
          {
            name: 'Create Groups',
            url: '/group-management/create-groups'
          },
          {
            name: 'Assign Users',
            url: '/group-management/assign-users'
          }
        ]
      },
      {
        name: 'Program<br>Management',
        url: '/program-management/program-setup',
        submenus: [
          {
            name: 'Manage Program',
            url: '/program-management/program-setup'
          },
          {
            name: 'Assignment by User',
            url: '/program-management/assignment-by-user'
          },
          {
            name: 'Assignment by Group',
            url: '/program-management/assignment-by-group'
          },
          {
            name: 'Assignment by Program',
            url: '/program-management/assignment-by-program'
          }
        ]
      },
      {
        name: 'Modeling<br>Activities',
        url: '/modeling-activities',
      },
      {
        name: 'System<br>Reports',
        url: '/system-reports/usage-reports',
      },
      {
        name: 'Visualizations',
        url: '/visualizations/apps',
        submenus: [
          {
            name: 'View Applications',
            url: '/visualizations/apps'
          },
          {
            name: 'Submit Application',
            url: '/visualizations/submit'
          }
        ]
      },
      {
        name: 'Computing<br>Environment',
        url: '/computing-environment'
      },
      {
        name: 'Submit<br>Dataset',
        url: '/submit-dataset',
      },
      {
        name: 'Admin<br>Tools',
        url: '/admin-tools/instance-management',
        submenus: [
          {
            name: 'Alert Banners',
            url: '/admin-tools/alert-banners'
          },
          {
            name: 'Instance Management',
            url: '/admin-tools/instance-management'
          }
        ]
      },
    ];

    var defaultUrls = {};
    defaultUrls[Role.buildInRoles.ADMINISTRATOR] = '/user-management/edit-account';
    defaultUrls[Role.buildInRoles.CURATOR] =       '/computing-environment';
    defaultUrls[Role.buildInRoles.ANALYST] =       '/computing-environment';
    defaultUrls[Role.buildInRoles.CONTRIBUTOR] =   '/submit-dataset';

    var obj = {};
    obj.menus = menus;
    obj.submenus = [];
    obj.publicPages = [
      '/password-reset',
      '/policy',
      '/forgot-password',
      '/terms',
      '/access-denied'
    ];
    obj.goDefaultUrl = function() {
      User.getCurrentUserRoles(function(roles) {
        for (var i = 0; i < roles.length; i++) {
          var url = defaultUrls[roles[i].name];
          if (url) {
            $location.path(url);
            return;
          }
        }

        if (User.isUserLoggedIn()) {
          $location.path('/my-account');
        } else {
          $location.path('/access-denied');
        }
      });
    };
    obj.getAccess = function(url){
      return paths.paths[url].access;
    };
    obj.getSubmenus = function(url) {
      obj.submenus = [];
      for (var i = 0; i < obj.menus.length; i++) {
        var item = obj.menus[i];
        item.active = (url === item.url) || (url.indexOf(item.url + '/') === 0);
        if (item.submenus === undefined) {
          continue;
        }

        for (var j = 0; j < item.submenus.length; j++) {
          var submenu = item.submenus[j];
          submenu.active = (url === submenu.url);
          if (submenu.active) {
            item.active = true;
            obj.submenus = item.submenus;
          }
        }
      }
    };
    obj.logout = function() {
      $http.get(Settings.OAUTH_URL + '/oauth/revoke').success(function() {
        User.dropCurrentUser();
        document.location.href = Settings.OAUTH_URL + '/oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=' + document.location.href;
      });
    };

    // init --------------------------------------------------------
    if (obj.publicPages.indexOf($location.path()) === -1) {
      User.getCurrentUserRoles(function() {
        obj.getSubmenus($location.path());
      });
    }
    // init --------------------------------------------------------

    return obj;

  });
