'use strict';

var pathsProviderFunc = function(){

  var obj = {};

  obj.paths = {
    '/user-management/create-account': {
      path:        '/user-management/create-account',
      templateUrl: 'views/user-management/create-account.html',
      controller:  'UserManagementCreateAccountCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },
    '/user-management/edit-account': {
      path:        '/user-management/edit-account/:dn?',
      templateUrl: 'views/user-management/edit-account.html',
      controller:  'UserManagementEditAccountCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },
    '/user-management/manage-permissions': {
      path:        '/user-management/manage-permissions/:dn?',
      templateUrl: 'views/user-management/manage-permissions.html',
      controller:  'UserManagementManagePermissionsCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },

    '/role-management/create-roles': {
      path:        '/role-management/create-roles',
      templateUrl: 'views/role-management/create-roles.html',
      controller:  'RoleManagementCreateRolesCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },
    '/role-management/assign-modeling-activities': {
      path:        '/role-management/assign-modeling-activities',
      templateUrl: 'views/role-management/assign-modeling-activities.html',
      controller:  'RoleManagementAssignModelingActivitiesCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },

    '/group-management/create-groups': {
      path:        '/group-management/create-groups',
      templateUrl: 'views/group-management/create-groups.html',
      controller:  'GroupManagementCreateGroupsCtrl',
      access:      { requiredRoles: ['ZZZ_Unused'] }
    },
    '/group-management/assign-users': {
      path:        '/group-management/assign-users',
      templateUrl: 'views/group-management/assign-users.html',
      controller:  'GroupManagementAssignUsersCtrl',
      access:      { requiredRoles: ['ZZZ_Unused'] }
    },


    '/program-management/program-setup': {
      path:        '/program-management/program-setup',
      templateUrl: 'views/program-management/program-setup.html',
      controller:  'ProgramManagementProgramSetupCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },
    '/program-management/assignment-by-user': {
      path:        '/program-management/assignment-by-user',
      templateUrl: 'views/program-management/assignment-by-user.html',
      controller:  'ProgramManagementAssignmentByUserCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },
    '/program-management/assignment-by-group': {
      path:        '/program-management/assignment-by-group',
      templateUrl: 'views/program-management/assignment-by-group.html',
      controller:  'ProgramManagementAssignmentByGroupCtrl',
      access:      { requiredRoles: ['ZZZ_Unused'] }
    },
    '/program-management/assignment-by-program': {
      path:        '/program-management/assignment-by-program',
      templateUrl: 'views/program-management/assignment-by-program.html',
      controller:  'ProgramManagementAssignmentByProgramCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },

    '/modeling-activities': {
      path:        '/modeling-activities',
      templateUrl: 'views/modeling-activities.html',
      controller:  'ModelingActivitiesCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },

    '/system-reports/usage-reports': {
      path:        '/system-reports/usage-reports',
      templateUrl: 'views/system-reports/usage-reports.html',
      controller:  'UsageReportsSystemReportsCtrl',
      access:      { requiredRoles: ['GHAP Administrator', 'Reporting'] }
    },
    '/system-reports/audit-reports': {
      path:        '/system-reports/audit-reports',
      templateUrl: 'views/system-reports/audit-reports.html',
      controller:  'AuditReportsSystemReportsCtrl',
      access:      { requiredRoles: ['GHAP Administrator', 'Reporting'] }
    },

    '/computing-environment': {
      path:        '/computing-environment',
      templateUrl: 'views/computing-environment.html',
      controller:  'ComputingEnvironmentCtrl',
      access:      { requiredRoles: ['GHAP Administrator', 'Data Analyst', 'Data Curator'] }
    },

    '/submit-dataset': {
      path:        '/submit-dataset',
      templateUrl: 'views/submit-dataset.html',
      controller:  'SubmitDatasetCtrl',
      access:      { requiredRoles: ['GHAP Administrator', 'Data Contributor', 'Data Curator'] }
    },

    '/visualizations/apps': {
      path:        '/visualizations/apps',
      templateUrl: 'views/visualizations/apps.html',
      controller:  'VisualizationsAppsCtrl',
      access:      { requiredRoles: ['GHAP Administrator', 'Data Viewer', 'Data Visualization Publisher'] }
    },
    '/visualizations/submit': {
      path:        '/visualizations/submit',
      templateUrl: 'views/visualizations/submit.html',
      controller:  'VisualizationsSubmitCtrl',
      access:      { requiredRoles: ['GHAP Administrator', 'BMGF Administrator', 'Administrators', 'Data Visualization Publisher'] }
    },

    '/admin-tools/alert-banners': {
      path:        '/admin-tools/alert-banners',
      templateUrl: 'views/admin-tools/alert-banners.html',
      controller:  'AlertBannersCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },

    '/admin-tools/instance-management': {
      path:        '/admin-tools/instance-management',
      templateUrl: 'views/admin-tools/instance-management.html',
      controller:  'InstanceManagementCtrl',
      access:      { requiredRoles: ['GHAP Administrator'] }
    },

    '/my-account': {
      path:        '/my-account',
      templateUrl: 'views/my-account.html',
      controller:  'MyAccountCtrl',
      access:      { requiresLogin: true }
    },

    '/forgot-password': {
      path:        '/forgot-password',
      templateUrl: 'views/forgot-password.html',
      controller: 'ForgotPasswordCtrl'
    },
    '/password-reset': {
      path:        '/password-reset',
      templateUrl: 'views/password-reset.html',
      controller:  'PasswordResetCtrl'
    },

    '/policy': {
      path:        '/policy',
      templateUrl: 'views/policy.html',
      controller:  'PolicyCtrl'
    },
    '/terms': {
      path:        '/terms',
      templateUrl: 'views/terms.html',
      controller:  'TermsCtrl'
    },
    '/access-denied': {
      path:        '/access-denied',
      templateUrl: 'views/access-denied.html',
      controller:  'AccessDeniedCtrl'
    }
  };

  obj.$get = function() {
    return pathsProviderFunc();
  };

  return obj;

};

/**
 * @ngdoc service
 * @name bmgfApp.paths
 * @description
 * # paths
 * Service in the bmgfApp.
 */

 angular.module('bmgfApp')
  .provider('paths', pathsProviderFunc)

  ;