// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [ // the first one is the default format
    all:           '*/*', // 'all' maps to '*' or the first available format in withFormat
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    hal:           ['application/hal+json','application/hal+xml'],
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}


grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password', 'client_secret']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false

ghap.password.expired.redirect.page = 'https://www.devtest.ghap.io/#/password-reset?re=true'
ghap.tos.page = 'https://www.devtest.ghap.io/#/terms'
ghap.forgot.pwd.page = 'https://www.devtest.ghap.io/#/forgot-password'
ghap.token.expire.minutes = 30
ghap.openid.client.id = 'projectservice'
ghap.keystore.file = 'ghap.samba.keystore.jks'
ghap.keystore.key = 'ghap'
ghap.keystore.password = ''

environments {
    development {
        grails.logging.jul.usebridge = true
        ghap.client.secret = ''
        grails.plugin.springsecurity.ldap.context.managerDn = 'CN=Administrator,CN=Users,DC=ad,DC=loc'
        grails.plugin.springsecurity.ldap.context.managerPassword = ''
        grails.plugin.springsecurity.ldap.context.server = 'ldap://192.168.56.101:389'
        grails.plugin.springsecurity.successHandler.defaultTargetUrl = 'https://www.dev.ghap.io'
        grails.serverURL = "http://localhost:8090/${appName}"
        ghap.banner.service.url = 'https://activityservice.samba.ghap.io/rest/v1/banner/'
    }
    samba {
        ghap.client.secret = ''
        grails.plugin.springsecurity.ldap.context.managerDn = 'CN=Administrator,CN=Users,DC=prod,DC=ghap,dc=io'
        grails.plugin.springsecurity.ldap.context.managerPassword = ''
        grails.plugin.springsecurity.ldap.context.server = 'ldap://10.10.12.100:389'
        grails.logging.jul.usebridge = false

        ghap.password.expired.redirect.page = 'https://www.samba.ghap.io/#/password-reset?re=true'
        ghap.tos.page = 'https://www.samba.ghap.io/#/terms'
        ghap.forgot.pwd.page = 'https://www.samba.ghap.io/#/forgot-password'
        grails.plugin.springsecurity.successHandler.defaultTargetUrl = 'https://www.samba.ghap.io'
        grails.serverURL = "https://oauth.samba.ghap.io"
        ghap.banner.service.url = 'https://activityservice.samba.ghap.io/rest/v1/banner/'
    }
    qa {
        ghap.client.secret = ''
        grails.plugin.springsecurity.ldap.context.managerDn = 'CN=GHAPAdministrator,CN=Users,DC=prod,DC=ghap,DC=io'
        grails.plugin.springsecurity.ldap.context.managerPassword = ''
        grails.plugin.springsecurity.ldap.context.server = 'ldap://10.10.12.100:389'
        grails.logging.jul.usebridge = false

        ghap.password.expired.redirect.page = 'https://www.qa.ghap.io/#/password-reset?re=true'
        ghap.tos.page = 'https://www.qa.ghap.io/#/terms'
        ghap.forgot.pwd.page = 'https://www.qa.ghap.io/#/forgot-password'
        grails.plugin.springsecurity.successHandler.defaultTargetUrl = 'https://www.qa.ghap.io'
        grails.serverURL = "https://oauth.qa.ghap.io"
        ghap.banner.service.url = 'https://activityservice.qa.ghap.io/rest/v1/banner/'
    }
    devtest {
        ghap.client.secret = ''
        grails.plugin.springsecurity.ldap.context.managerDn = 'CN=Administrator,CN=Users,DC=prod,DC=ghap,DC=io'
        grails.plugin.springsecurity.ldap.context.managerPassword = ''
        grails.plugin.springsecurity.ldap.context.server = 'ldap://10.10.12.101:389'

        ghap.password.expired.redirect.page = 'https://www.dev.ghap.io/#/password-reset?re=true'
        ghap.tos.page = 'https://www.dev.ghap.io/#/terms'
        ghap.forgot.pwd.page = 'https://www.dev.ghap.io/#/forgot-password'
        grails.plugin.springsecurity.successHandler.defaultTargetUrl = 'https://www.dev.ghap.io'
        ghap.banner.service.url = 'https://activityservice.samba.ghap.io/rest/v1/banner/'
    }
    production {
        ghap.client.secret = ''
        grails.plugin.springsecurity.ldap.context.managerDn = 'CN=GHAPAdministrator,CN=Users,DC=prod,DC=ghap,DC=io'
        grails.plugin.springsecurity.ldap.context.managerPassword = ''
        grails.plugin.springsecurity.ldap.context.server = 'ldap://10.10.12.100:389'
        grails.logging.jul.usebridge = false

        ghap.password.expired.redirect.page = 'https://www.ghap.io/#/password-reset?re=true'
        ghap.tos.page = 'https://www.ghap.io/#/terms'
        ghap.forgot.pwd.page = 'https://www.ghap.io/#/forgot-password'
        grails.plugin.springsecurity.successHandler.defaultTargetUrl = 'https://www.ghap.io'
        grails.serverURL = "https://oauth.ghap.io"
        ghap.banner.service.url = 'https://activityservice.ghap.io/rest/v1/banner/'
    }
}

// log4j configuration
log4j.main = {
    // Example of changing the log pattern for the default console appender:
    //
    appenders {
        rollingFile name:'catalinaOut', maxFileSize:102400000, file:"${System.properties.getProperty('catalina.home')}/logs/oauth.log"
    }

    root {
        debug 'catalinaOut', 'stdout'
        additivity = true
    }

    debug 'grails.app',
           'io.ghap',
           'org.springframework'
    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh',       // layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping',        // URL mapping
           'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugin',            // plugin
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
}


// Added by the Spring Security OAuth2 Provider plugin:
grails.plugin.springsecurity.oauthProvider.clientLookup.className = 'io.ghap.oauth.Client'
grails.plugin.springsecurity.oauthProvider.authorizationCodeLookup.className = 'io.ghap.oauth.AuthorizationCode'
grails.plugin.springsecurity.oauthProvider.accessTokenLookup.className = 'io.ghap.oauth.AccessToken'
grails.plugin.springsecurity.oauthProvider.refreshTokenLookup.className = 'io.ghap.oauth.RefreshToken'
grails.plugin.springsecurity.oauthProvider.tokenServices.accessTokenValiditySeconds = 60 * 60 * 12
grails.plugin.springsecurity.oauthProvider.tokenServices.refreshTokenValiditySeconds = 60 * 60 * 24 * 30
grails.plugin.springsecurity.oauthProvider.approval.auto = null

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'io.ghap.oauth.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'io.ghap.oauth.UserRole'
grails.plugin.springsecurity.authority.className = 'io.ghap.oauth.Role'
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
	'/':                              ['permitAll'],
	'/index':                         ['permitAll'],
	'/index.gsp':                     ['permitAll'],
	'/token/**':                     ['permitAll'],
	'/**/js/**':                      ['permitAll'],
	'/**/assets/**':                      ['permitAll'],
	'/**/css/**':                     ['permitAll'],
	'/**/images/**':                  ['permitAll'],
	'/**/favicon.ico':                ['permitAll'],
	'/openIdConfiguration/**':                ['permitAll'],
	'/health/**':                      ['permitAll'],
    '/oauth/authorize.dispatch':      ["isFullyAuthenticated() and (request.getMethod().equals('GET') or request.getMethod().equals('POST'))"],
    '/oauth/token.dispatch':          ["isFullyAuthenticated() and request.getMethod().equals('POST')"]
]

grails.plugin.springsecurity.filterChain.chainMap = [
        '/oauth/token': 'JOINED_FILTERS,-oauth2ProviderFilter,-securityContextPersistenceFilter,-logoutFilter,-rememberMeAuthenticationFilter,-exceptionTranslationFilter',
        '/securedOAuth2Resources/**': 'JOINED_FILTERS,-securityContextPersistenceFilter,-logoutFilter,-rememberMeAuthenticationFilter,-exceptionTranslationFilter',
        '/**': 'JOINED_FILTERS,-statelessSecurityContextPersistenceFilter,-oauth2ProviderFilter,-clientCredentialsTokenEndpointFilter,-oauth2ExceptionTranslationFilter'
]

//Active directory setup, read the spring-security-ldap docs for more information
grails.plugin.springsecurity.providerNames = ['ldapAuthProvider']
grails.plugin.springsecurity.ldap.authorities.retrieveGroupRoles = true
grails.plugin.springsecurity.ldap.authorities.retrieveDatabaseRoles = false
grails.plugin.springsecurity.ldap.mapper.userDetailsClass = 'person'
grails.plugin.springsecurity.ldap.search.filter = '(sAMAccountName={0})'
grails.plugin.springsecurity.ldap.search.base = 'DC=prod,DC=ghap,DC=io'
grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = true
grails.plugin.springsecurity.ldap.authorities.groupSearchBase = 'DC=prod,DC=ghap,DC=io'
grails.plugin.springsecurity.ldap.authorities.groupRoleAttribute = 'cn'
grails.plugin.springsecurity.ldap.authorities.groupSearchFilter = '(&(objectClass=group)(member={0}))'
grails.plugin.springsecurity.ldap.authorities.searchSubtree = true
grails.plugin.springsecurity.ldap.authorities.clean.prefix = 'CN='
grails.plugin.springsecurity.ldap.auth.hideUserNotFoundExceptions = true
grails.plugin.springsecurity.ldap.auth.useAuthPassword = true
//grails.plugin.springsecurity.ldap.authenticator.useBind = false
//grails.plugin.springsecurity.ldap.authenticator.passwordAttributeName = 'unicodePwd'
environments {
    development {
        grails.plugin.springsecurity.ldap.search.base = 'DC=ad,DC=loc'
        grails.plugin.springsecurity.ldap.authorities.groupSearchBase = 'DC=ad,DC=loc'
    }
}
